package vn.scarlet.cinema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Chuẩn bị cơ sở dữ liệu lúc web khởi động (trước khi web nhận yêu cầu), để đưa web sang máy khác hay máy chủ khác
 * không phải tự tay chạy lệnh SQL. Làm lần lượt ba việc:
 *
 * 1. Cơ sở dữ liệu CHƯA CÓ BẢNG NÀO của rạp: tạo 7 bảng và nạp dữ liệu mẫu từ hai file db/schema.sql, db/du-lieu-mau.sql
 *    nằm sẵn trong web. Đã có dù chỉ một bảng thì bỏ qua, không bao giờ xóa hay nạp đè dữ liệu đang có.
 * 2. Cơ sở dữ liệu tạo từ bản cũ, bảng movies chưa có cột trailer_url: thêm cột và điền trailer cho các phim mẫu.
 * 3. Có đặt biến môi trường ADMIN_PASSWORD: đặt mật khẩu đó cho tài khoản quản trị (trong bảng chỉ lưu chuỗi đã băm).
 */
@Component
@DependsOnDatabaseInitialization
public class DatabaseUpgrade implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUpgrade.class);

    /** Bảy bảng của rạp, xếp bảng con trước bảng cha (thứ tự xóa được khi phải dọn một lần tạo bị lỗi giữa chừng). */
    private static final List<String> TABLES =
            List.of("tickets", "bookings", "promotions", "showtimes", "rooms", "movies", "users");

    /** Tên khóa của MySQL, để hai máy chủ khởi động cùng lúc không cùng tạo bảng. */
    private static final String LOCK = "scarlet_cinema_khoi_tao";

    /** Trailer của các phim trong dữ liệu mẫu (tra ngày 06/10/2026), giống file du-lieu-mau.sql. */
    private static final Map<String, String> TRAILERS = new LinkedHashMap<>();

    static {
        TRAILERS.put("Hòn Đảo Quên Lãng", "https://www.youtube.com/watch?v=vd1wzfi8-HI");
        TRAILERS.put("Trại Buôn Người", "https://www.youtube.com/watch?v=HZXb3P3mitA");
        TRAILERS.put("Quyết Cua Anh Này", "https://www.youtube.com/watch?v=XKdEiTx9W68");
        TRAILERS.put("Lên Hương", "https://www.youtube.com/watch?v=VcAHmTOK-TI");
        TRAILERS.put("Scotty Giải Cứu Hoàng Thượng", "https://www.youtube.com/watch?v=yqYjXjDDthY");
        TRAILERS.put("Út Lan 2", "https://www.youtube.com/watch?v=f9fGXp9yan0");
        TRAILERS.put("Thần Sư Chung Quỳ: Linh Giới Đại Chiến", "https://www.youtube.com/watch?v=KEBDVoBTeTk");
        TRAILERS.put("Pháo Hoa Lúc Bình Minh", "https://www.youtube.com/watch?v=oXudBJeF1TI");
        TRAILERS.put("Án Mạng Xém Hoàn Hảo", "https://www.youtube.com/watch?v=IW-k9cMySWg");
        TRAILERS.put("Blue Lock: Cuộc Chiến Của Những Tiền Đạo", "https://www.youtube.com/watch?v=y4_4y7VaSTM");
        TRAILERS.put("Street Fighter", "https://www.youtube.com/watch?v=Xt4X4FvXk2A");
    }

    private final JdbcTemplate jdbc;
    private final String adminPassword;

    public DatabaseUpgrade(JdbcTemplate jdbc, @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        this.jdbc = jdbc;
        this.adminPassword = adminPassword == null ? "" : adminPassword.trim();
    }

    @Override
    public void afterPropertiesSet() {
        try {
            createTablesIfEmpty();
            addTrailerColumnIfMissing();
            applyAdminPassword();
        } catch (Exception e) {
            // Không dừng web vì lỗi này (ví dụ MySQL chưa bật): lớp DatabaseCheck sẽ báo rõ tình trạng cơ sở dữ liệu
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            log.error("KHONG CHUAN BI DUOC CO SO DU LIEU. Chi tiet: {}", root.toString());
        }
    }

    // ----- 1. Cơ sở dữ liệu trống thì tạo bảng và nạp dữ liệu mẫu -----

    private void createTablesIfEmpty() {
        if (countTables() > 0) {
            return;
        }
        // Mọi lệnh phải chạy trên CÙNG một kết nối: file dữ liệu mẫu dùng biến @hom_nay và bảng tạm, chỉ sống trong một kết nối
        jdbc.execute((ConnectionCallback<Void>) con -> {
            try (Statement st = con.createStatement()) {
                if (!getLock(st)) {
                    log.warn("Mot may chu khac dang tao co so du lieu, lan khoi dong nay bo qua buoc tao bang.");
                    return null;
                }
                try {
                    // Trong lúc chờ khóa, máy chủ kia có thể đã tạo xong: đếm lại rồi mới làm
                    if (countTables() > 0) {
                        return null;
                    }
                    log.info("Co so du lieu chua co bang nao cua rap: dang tao 7 bang va nap du lieu mau...");
                    try {
                        runScript(con, "db/schema.sql");
                        runScript(con, "db/du-lieu-mau.sql");
                    } catch (RuntimeException | SQLException e) {
                        // Lỗi giữa chừng thì xóa phần đã tạo, để lần khởi động sau làm lại từ đầu chứ không kẹt ở trạng thái dở dang
                        for (String table : TABLES) {
                            st.execute("DROP TABLE IF EXISTS " + table);
                        }
                        throw e;
                    }
                    log.info("Da tao co so du lieu va nap du lieu mau (lich chieu 7 ngay tinh tu hom nay).");
                } finally {
                    st.execute("SELECT RELEASE_LOCK('" + LOCK + "')");
                }
            }
            return null;
        });
    }

    /** Đếm xem cơ sở dữ liệu đang nối tới có mấy bảng trong 7 bảng của rạp. */
    private int countTables() {
        Integer found = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN "
                        + "('users', 'movies', 'rooms', 'showtimes', 'promotions', 'bookings', 'tickets')",
                Integer.class);
        return found == null ? 0 : found;
    }

    private boolean getLock(Statement st) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT GET_LOCK('" + LOCK + "', 120)")) {
            return rs.next() && rs.getInt(1) == 1;
        }
    }

    /**
     * Chạy một file SQL nằm trong web. Hai lệnh CREATE DATABASE và USE ở đầu file bị bỏ đi: web tạo bảng ngay trong
     * cơ sở dữ liệu mà DB_URL đang trỏ tới, tên gì cũng được (ví dụ defaultdb có sẵn trên Aiven).
     */
    private void runScript(Connection con, String path) throws SQLException {
        String sql;
        try {
            sql = StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Khong doc duoc file " + path, e);
        }
        sql = sql.replaceAll("(?im)^\\s*CREATE\\s+DATABASE\\b[^;]*;", "").replaceAll("(?im)^\\s*USE\\s+\\w+\\s*;", "");
        ScriptUtils.executeSqlScript(con,
                new EncodedResource(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
    }

    // ----- 2. Cơ sở dữ liệu tạo từ bản cũ: thêm cột trailer_url -----

    private void addTrailerColumnIfMissing() {
        // Bảng information_schema.columns liệt kê mọi cột của mọi bảng: đếm xem bảng movies đã có cột trailer_url chưa
        Integer found = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'movies' AND column_name = 'trailer_url'",
                Integer.class);
        if (found != null && found > 0) {
            return;
        }
        jdbc.execute("ALTER TABLE movies ADD COLUMN trailer_url VARCHAR(500) NULL "
                + "COMMENT 'Đường dẫn trailer trên YouTube; để trống nếu chưa có' AFTER backdrop_url");
        int filled = 0;
        for (Map.Entry<String, String> e : TRAILERS.entrySet()) {
            filled += jdbc.update("UPDATE movies SET trailer_url = ? WHERE title = ? AND trailer_url IS NULL",
                    e.getValue(), e.getKey());
        }
        log.info("Da nang cap co so du lieu: them cot movies.trailer_url, dien trailer cho {} phim.", filled);
    }

    // ----- 3. Mật khẩu quản trị lấy từ biến môi trường ADMIN_PASSWORD (nếu có đặt) -----

    private void applyAdminPassword() {
        if (adminPassword.isEmpty()) {
            return;
        }
        if (adminPassword.length() < 8) {
            log.warn("Bien ADMIN_PASSWORD ngan hon 8 ky tu nen bi bo qua, mat khau quan tri giu nguyen.");
            return;
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        List<Map<String, Object>> admins = jdbc.queryForList("SELECT id, password_hash FROM users WHERE role = 'ADMIN'");
        int changed = 0;
        for (Map<String, Object> admin : admins) {
            // Mật khẩu đang lưu đã khớp thì thôi, để mỗi lần khởi động không phải ghi lại
            if (!encoder.matches(adminPassword, String.valueOf(admin.get("password_hash")))) {
                changed += jdbc.update("UPDATE users SET password_hash = ? WHERE id = ?",
                        encoder.encode(adminPassword), admin.get("id"));
            }
        }
        if (changed > 0) {
            log.info("Da dat mat khau cho {} tai khoan quan tri theo bien ADMIN_PASSWORD.", changed);
        }
    }
}
