package vn.scarlet.cinema.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import vn.scarlet.cinema.model.Promotion;
import vn.scarlet.cinema.model.PromotionStat;

/** Các câu lệnh SQL trên bảng promotions (mã ưu đãi). */
@Repository
public class PromotionDao {

    private static final String COLUMNS = "p.id, p.code, p.title, p.description, p.discount_type, p.discount_value, p.max_discount, "
            + "p.min_total, p.start_date, p.end_date, p.usage_limit, p.active";

    /**
     * Một lượt dùng mã là một đơn đã thanh toán, hoặc một đơn đang giữ ghế còn hạn (khách đang ở bước thanh toán).
     * Đơn giữ ghế quá hạn bị xóa nên lượt của nó tự được trả lại.
     */
    private static final String IN_USE = "(b.status = 'PAID' OR b.expires_at > NOW())";

    private static final RowMapper<Promotion> MAPPER = (rs, rowNum) -> new Promotion(
            rs.getInt("id"),
            rs.getString("code"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("discount_type"),
            rs.getInt("discount_value"),
            MovieDao.intOrNull(rs, "max_discount"),
            rs.getInt("min_total"),
            rs.getObject("start_date", LocalDate.class),
            rs.getObject("end_date", LocalDate.class),
            MovieDao.intOrNull(rs, "usage_limit"),
            rs.getBoolean("active"));

    private final JdbcTemplate jdbc;

    public PromotionDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Trang chủ: các mã đang dùng được (đang bật, còn trong hạn, chưa hết lượt), nhiều nhất limit mã. */
    public List<Promotion> findUsable(int limit) {
        String sql = "SELECT " + COLUMNS + " FROM promotions p "
                + "WHERE p.active = 1 AND CURDATE() BETWEEN p.start_date AND p.end_date "
                + "  AND (p.usage_limit IS NULL OR p.usage_limit > "
                + "       (SELECT COUNT(*) FROM bookings b WHERE b.promotion_id = p.id AND " + IN_USE + ")) "
                + "ORDER BY p.id LIMIT ?";
        return jdbc.query(sql, MAPPER, limit);
    }

    /**
     * Lấy một mã theo chữ khách nhập và KHÓA dòng đó (FOR UPDATE) tới hết giao dịch.
     * Nhờ vậy hai người cùng áp một mã sắp hết lượt phải chờ nhau, không thể cùng lọt qua bước đếm lượt.
     */
    public Optional<Promotion> findByCodeForUpdate(String code) {
        String sql = "SELECT " + COLUMNS + " FROM promotions p WHERE p.code = ? FOR UPDATE";
        return jdbc.query(sql, MAPPER, code).stream().findFirst();
    }

    /** Số lượt mã đã được dùng, không tính đơn exceptBookingId (đơn đang áp mã). */
    public int countUses(int promotionId, int exceptBookingId) {
        String sql = "SELECT COUNT(*) FROM bookings b WHERE b.promotion_id = ? AND b.id <> ? AND " + IN_USE;
        return jdbc.queryForObject(sql, Integer.class, promotionId, exceptBookingId);
    }

    /** Số lượt một khách đã dùng mã này, không tính đơn exceptBookingId. */
    public int countUsesByUser(int promotionId, int userId, int exceptBookingId) {
        String sql = "SELECT COUNT(*) FROM bookings b WHERE b.promotion_id = ? AND b.user_id = ? AND b.id <> ? AND " + IN_USE;
        return jdbc.queryForObject(sql, Integer.class, promotionId, userId, exceptBookingId);
    }

    /** Trang quản trị: từng mã đã được dùng bao nhiêu lượt, giảm tổng cộng bao nhiêu (chỉ tính đơn đã thanh toán). */
    public List<PromotionStat> stats() {
        String sql = "SELECT p.code, p.title, p.usage_limit, p.end_date, p.active, COUNT(b.id) AS uses, "
                + "       COALESCE(SUM(b.discount_amount), 0) AS total_discount "
                + "FROM promotions p "
                + "LEFT JOIN bookings b ON b.promotion_id = p.id AND b.status = 'PAID' "
                + "GROUP BY p.id, p.code, p.title, p.usage_limit, p.end_date, p.active "
                + "ORDER BY p.id";
        return jdbc.query(sql, (rs, rowNum) -> new PromotionStat(
                rs.getString("code"),
                rs.getString("title"),
                MovieDao.intOrNull(rs, "usage_limit"),
                rs.getObject("end_date", LocalDate.class),
                rs.getBoolean("active"),
                rs.getInt("uses"),
                rs.getLong("total_discount")));
    }
}
