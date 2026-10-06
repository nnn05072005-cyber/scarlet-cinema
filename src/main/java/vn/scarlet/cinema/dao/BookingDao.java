package vn.scarlet.cinema.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import vn.scarlet.cinema.model.BookingView;
import vn.scarlet.cinema.model.MovieRevenue;

/**
 * Các câu lệnh SQL trên bảng bookings (đơn đặt vé) và tickets (vé).
 *
 * Vòng đời một đơn: tạo ra ở trạng thái PENDING (giữ ghế, chờ thanh toán), thanh toán xong thành PAID.
 * Đơn PENDING quá hạn hoặc bị khách hủy thì bị xóa; các vé của đơn bị xóa theo nên ghế được nhả.
 *
 * Tiền: total_amount là số tiền phải trả. Khi áp mã ưu đãi, discount_amount ghi số tiền được giảm và
 * total_amount bị trừ đi đúng số đó, nên tiền vé gốc luôn bằng total_amount + discount_amount.
 */
@Repository
public class BookingDao {

    /**
     * Phần chung của các câu SELECT: đơn đặt vé nối với khách, suất chiếu, phim, phòng và các vé.
     * GROUP_CONCAT gộp các ghế của một đơn thành một chuỗi "E4, E5, E6", xếp theo hàng rồi theo số ghế.
     */
    private static final String SELECT = "SELECT b.id, b.booking_code, b.user_id, u.full_name, b.showtime_id, s.movie_id, m.title, "
            + "       m.age_rating, m.poster_url, s.start_time, r.name AS room_name, "
            + "       GROUP_CONCAT(t.seat_code ORDER BY LEFT(t.seat_code, 1), CAST(SUBSTRING(t.seat_code, 2) AS UNSIGNED) "
            + "                    SEPARATOR ', ') AS seats, "
            + "       COUNT(t.id) AS ticket_count, b.total_amount, b.created_at, b.status, b.payment_method, b.paid_at, "
            + "       TIMESTAMPDIFF(SECOND, NOW(), b.expires_at) AS seconds_left, b.discount_amount, p.code AS promo_code "
            + "FROM bookings b "
            + "JOIN users     u ON u.id = b.user_id "
            + "JOIN showtimes s ON s.id = b.showtime_id "
            + "JOIN movies    m ON m.id = s.movie_id "
            + "JOIN rooms     r ON r.id = s.room_id "
            + "JOIN tickets   t ON t.booking_id = b.id "
            + "LEFT JOIN promotions p ON p.id = b.promotion_id ";

    private static final String GROUP_BY = "GROUP BY b.id, b.booking_code, b.user_id, u.full_name, b.showtime_id, s.movie_id, m.title, "
            + "         m.age_rating, m.poster_url, s.start_time, r.name, b.total_amount, b.created_at, b.status, "
            + "         b.payment_method, b.paid_at, b.expires_at, b.discount_amount, p.code ";

    private static final RowMapper<BookingView> MAPPER = (rs, rowNum) -> new BookingView(
            rs.getInt("id"),
            rs.getString("booking_code"),
            rs.getInt("user_id"),
            rs.getString("full_name"),
            rs.getInt("showtime_id"),
            rs.getInt("movie_id"),
            rs.getString("title"),
            rs.getString("age_rating"),
            rs.getString("poster_url"),
            rs.getObject("start_time", LocalDateTime.class),
            rs.getString("room_name"),
            rs.getString("seats"),
            rs.getInt("ticket_count"),
            rs.getInt("total_amount"),
            rs.getObject("created_at", LocalDateTime.class),
            rs.getString("status"),
            rs.getString("payment_method"),
            rs.getObject("paid_at", LocalDateTime.class),
            MovieDao.intOrNull(rs, "seconds_left"),
            rs.getInt("discount_amount"),
            rs.getString("promo_code"));

    private final JdbcTemplate jdbc;

    public BookingDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Thêm đơn đặt vé ở trạng thái chờ thanh toán (PENDING), ghế được giữ trong holdMinutes phút.
     * Mã đơn lúc này là mã tạm. Trả về mã số (id) của đơn vừa tạo.
     */
    public int insertBooking(String temporaryCode, int userId, int showtimeId, int totalAmount, int holdMinutes) {
        String sql = "INSERT INTO bookings (booking_code, user_id, showtime_id, total_amount, status, created_at, expires_at) "
                + "VALUES (?, ?, ?, ?, 'PENDING', NOW(), NOW() + INTERVAL ? MINUTE)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, temporaryCode);
            ps.setInt(2, userId);
            ps.setInt(3, showtimeId);
            ps.setInt(4, totalAmount);
            ps.setInt(5, holdMinutes);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().intValue();
    }

    /** Đổi mã tạm thành mã đặt vé chính thức, ví dụ SC-20261005-0147. */
    public void updateCode(int bookingId, String bookingCode) {
        jdbc.update("UPDATE bookings SET booking_code = ? WHERE id = ?", bookingCode, bookingId);
    }

    /**
     * Thêm một vé (một ghế). Nếu ghế này của suất chiếu đã có người đặt,
     * ràng buộc UNIQUE (showtime_id, seat_code) làm MySQL báo lỗi trùng và lệnh này ném ngoại lệ.
     */
    public void insertTicket(int bookingId, int showtimeId, String seatCode, int price) {
        String sql = "INSERT INTO tickets (booking_id, showtime_id, seat_code, price) VALUES (?, ?, ?, ?)";
        jdbc.update(sql, bookingId, showtimeId, seatCode, price);
    }

    /**
     * Thanh toán: chuyển đơn từ PENDING sang PAID. Lệnh chỉ có tác dụng khi đơn là của đúng khách,
     * còn chờ thanh toán và còn hạn giữ ghế. Trả về 1 nếu thanh toán được, 0 nếu không.
     */
    public int markPaid(String bookingCode, int userId, String paymentMethod) {
        String sql = "UPDATE bookings SET status = 'PAID', payment_method = ?, paid_at = NOW() "
                + "WHERE booking_code = ? AND user_id = ? AND status = 'PENDING' AND expires_at > NOW()";
        return jdbc.update(sql, paymentMethod, bookingCode, userId);
    }

    /**
     * Áp mã ưu đãi cho một đơn đang chờ thanh toán. subtotal là tiền vé gốc của đơn.
     * Ghi cả ba cột trong một lệnh nên đơn không bao giờ ở trạng thái dở dang. Trả về 1 nếu áp được.
     */
    public int setPromotion(int bookingId, int promotionId, int discount, int subtotal) {
        String sql = "UPDATE bookings SET promotion_id = ?, discount_amount = ?, total_amount = ? "
                + "WHERE id = ? AND status = 'PENDING' AND expires_at > NOW()";
        return jdbc.update(sql, promotionId, discount, subtotal - discount, bookingId);
    }

    /**
     * Bỏ mã ưu đãi khỏi đơn đang chờ thanh toán. Trong lệnh UPDATE một bảng, MySQL gán lần lượt từ trái sang phải:
     * trả lại tiền vé gốc trước (lúc này discount_amount còn giá trị cũ), rồi mới xóa số tiền giảm.
     */
    public int clearPromotion(String bookingCode, int userId) {
        String sql = "UPDATE bookings SET total_amount = total_amount + discount_amount, discount_amount = 0, promotion_id = NULL "
                + "WHERE booking_code = ? AND user_id = ? AND status = 'PENDING' AND promotion_id IS NOT NULL";
        return jdbc.update(sql, bookingCode, userId);
    }

    /** Khách hủy đơn chưa thanh toán: xóa đơn, các vé của đơn bị xóa theo (ON DELETE CASCADE). */
    public int deletePending(String bookingCode, int userId) {
        String sql = "DELETE FROM bookings WHERE booking_code = ? AND user_id = ? AND status = 'PENDING'";
        return jdbc.update(sql, bookingCode, userId);
    }

    /** Nhả ghế: xóa mọi đơn chờ thanh toán đã quá hạn giữ ghế. Trả về số đơn đã xóa. */
    public int deleteExpired() {
        return jdbc.update("DELETE FROM bookings WHERE status = 'PENDING' AND expires_at <= NOW()");
    }

    /** Đơn đang chờ thanh toán (còn hạn) của một khách ở một suất chiếu, nếu có. */
    public Optional<BookingView> findPending(int userId, int showtimeId) {
        String sql = SELECT + "WHERE b.user_id = ? AND b.showtime_id = ? AND b.status = 'PENDING' AND b.expires_at > NOW() "
                + GROUP_BY + "ORDER BY b.id DESC";
        return jdbc.query(sql, MAPPER, userId, showtimeId).stream().findFirst();
    }

    /** Tìm đơn đặt vé theo mã in trên vé. */
    public Optional<BookingView> findByCode(String code) {
        String sql = SELECT + "WHERE b.booking_code = ? " + GROUP_BY;
        return jdbc.query(sql, MAPPER, code).stream().findFirst();
    }

    /** Lịch sử vé của một khách, đơn mới nhất ở trên. */
    public List<BookingView> findByUser(int userId) {
        String sql = SELECT + "WHERE b.user_id = ? " + GROUP_BY + "ORDER BY b.created_at DESC, b.id DESC";
        return jdbc.query(sql, MAPPER, userId);
    }

    /** Trang quản trị: các đơn mới nhất, tìm theo mã vé hoặc tên khách (chuỗi rỗng là lấy tất cả). */
    public List<BookingView> search(String keyword, int limit) {
        String sql = SELECT
                + "WHERE b.booking_code LIKE CONCAT('%', ?, '%') OR u.full_name LIKE CONCAT('%', ?, '%') "
                + GROUP_BY
                + "ORDER BY b.created_at DESC, b.id DESC LIMIT ?";
        return jdbc.query(sql, MAPPER, keyword, keyword, limit);
    }

    /**
     * Tổng số đơn, số vé, doanh thu và tổng tiền đã giảm của cả rạp. Chỉ tính đơn đã thanh toán.
     * Doanh thu là số tiền khách thật sự trả, nên cộng theo đơn; bảng con x đếm số vé của từng đơn
     * để một đơn nhiều vé không bị cộng tiền nhiều lần.
     */
    public Map<String, Object> totals() {
        String sql = "SELECT COUNT(*) AS bookings, COALESCE(SUM(x.tickets), 0) AS tickets, "
                + "       COALESCE(SUM(b.total_amount), 0) AS revenue, COALESCE(SUM(b.discount_amount), 0) AS discount "
                + "FROM bookings b "
                + "JOIN (SELECT booking_id, COUNT(*) AS tickets FROM tickets GROUP BY booking_id) x ON x.booking_id = b.id "
                + "WHERE b.status = 'PAID'";
        return jdbc.queryForMap(sql);
    }

    /** Số vé và doanh thu (đã trừ giảm giá) theo từng phim, phim bán chạy nhất ở trên. Chỉ tính đơn đã thanh toán. */
    public List<MovieRevenue> revenueByMovie() {
        String sql = "SELECT m.title, SUM(x.tickets) AS tickets, COALESCE(SUM(b.total_amount), 0) AS revenue "
                + "FROM bookings b "
                + "JOIN (SELECT booking_id, COUNT(*) AS tickets FROM tickets GROUP BY booking_id) x ON x.booking_id = b.id "
                + "JOIN showtimes s ON s.id = b.showtime_id "
                + "JOIN movies    m ON m.id = s.movie_id "
                + "WHERE b.status = 'PAID' "
                + "GROUP BY m.id, m.title "
                + "ORDER BY revenue DESC, m.title";
        return jdbc.query(sql, (rs, rowNum) -> new MovieRevenue(rs.getString("title"), rs.getInt("tickets"), rs.getLong("revenue")));
    }
}
