package vn.scarlet.cinema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Chạy một lần ngay sau khi web khởi động: thử đọc cơ sở dữ liệu và ghi kết quả ra màn hình,
 * để biết ngay MySQL đã bật và đã có dữ liệu chưa.
 */
@Component
public class DatabaseCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCheck.class);

    private final JdbcTemplate jdbc;

    public DatabaseCheck(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer movies = jdbc.queryForObject("SELECT COUNT(*) FROM movies", Integer.class);
            Integer showtimes = jdbc.queryForObject("SELECT COUNT(*) FROM showtimes WHERE start_time > NOW()", Integer.class);
            String version = jdbc.queryForObject("SELECT VERSION()", String.class);
            log.info("Da ket noi MySQL ({}): {} phim, {} suat chieu sap toi.", version, movies, showtimes);
            if (showtimes != null && showtimes == 0) {
                log.warn("Khong con suat chieu nao sap toi. Hay chay lai file du-lieu-mau.sql de tao lich chieu moi.");
            }
        } catch (Exception e) {
            // Lấy nguyên nhân gốc (ví dụ: MySQL chưa bật, sai mật khẩu, chưa có cơ sở dữ liệu)
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            log.error("KHONG DOC DUOC CO SO DU LIEU. Hay bat MySQL (XAMPP) roi chay lai web; kiem tra DB_URL, DB_USER, DB_PASSWORD neu dung MySQL khac. Chi tiet: {}", root.toString());
        }
    }
}
