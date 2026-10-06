package vn.scarlet.cinema.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import vn.scarlet.cinema.model.ShowtimeView;

/** Các câu lệnh SQL trên bảng showtimes (và đếm vé đã bán trong bảng tickets). */
@Repository
public class ShowtimeDao {

    /**
     * Phần chung của các câu SELECT: suất chiếu nối với phim và phòng, đếm số vé đã bán.
     * Số ghế còn = tổng số ghế của phòng - số vé đã bán.
     */
    private static final String SELECT = "SELECT s.id, s.movie_id, m.title, m.age_rating, m.duration_min, m.poster_url, "
            + "       s.room_id, r.name AS room_name, r.seat_rows, r.seats_per_row, s.start_time, s.price, "
            + "       COUNT(t.id) AS sold "
            + "FROM showtimes s "
            + "JOIN movies m ON m.id = s.movie_id "
            + "JOIN rooms  r ON r.id = s.room_id "
            + "LEFT JOIN tickets t ON t.showtime_id = s.id ";

    private static final String GROUP_BY = "GROUP BY s.id, s.movie_id, m.title, m.age_rating, m.duration_min, m.poster_url, "
            + "         s.room_id, r.name, r.seat_rows, r.seats_per_row, s.start_time, s.price ";

    private static final RowMapper<ShowtimeView> MAPPER = (rs, rowNum) -> new ShowtimeView(
            rs.getInt("id"),
            rs.getInt("movie_id"),
            rs.getString("title"),
            rs.getString("age_rating"),
            MovieDao.intOrNull(rs, "duration_min"),
            rs.getString("poster_url"),
            rs.getInt("room_id"),
            rs.getString("room_name"),
            rs.getInt("seat_rows"),
            rs.getInt("seats_per_row"),
            rs.getObject("start_time", LocalDateTime.class),
            rs.getInt("price"),
            rs.getInt("sold"));

    private final JdbcTemplate jdbc;

    public ShowtimeDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ShowtimeView> findById(int id) {
        String sql = SELECT + "WHERE s.id = ? " + GROUP_BY;
        return jdbc.query(sql, MAPPER, id).stream().findFirst();
    }

    /** Mọi suất chiếu bắt đầu trong khoảng [from, to), xếp theo giờ chiếu. */
    public List<ShowtimeView> findBetween(LocalDateTime from, LocalDateTime to) {
        String sql = SELECT + "WHERE s.start_time >= ? AND s.start_time < ? " + GROUP_BY + "ORDER BY s.start_time, s.room_id";
        return jdbc.query(sql, MAPPER, from, to);
    }

    /** Suất chiếu của một phim bắt đầu trong khoảng [from, to). */
    public List<ShowtimeView> findByMovieBetween(int movieId, LocalDateTime from, LocalDateTime to) {
        String sql = SELECT + "WHERE s.movie_id = ? AND s.start_time >= ? AND s.start_time < ? " + GROUP_BY
                + "ORDER BY s.start_time, s.room_id";
        return jdbc.query(sql, MAPPER, movieId, from, to);
    }

    /** Giờ bắt đầu của suất chiếu gần nhất sau thời điểm đã cho; null nếu không còn suất nào. */
    public LocalDateTime findNextStartTime(LocalDateTime after) {
        return jdbc.queryForObject("SELECT MIN(start_time) FROM showtimes WHERE start_time > ?", LocalDateTime.class, after);
    }

    /** Các ghế đã bán của một suất chiếu (để tô màu "đã bán" trên sơ đồ ghế). */
    public List<String> findSoldSeats(int showtimeId) {
        return jdbc.queryForList("SELECT seat_code FROM tickets WHERE showtime_id = ?", String.class, showtimeId);
    }

    /**
     * Kiểm tra trùng giờ trong cùng một phòng trước khi thêm hoặc sửa suất chiếu.
     * Hai suất trùng nhau khi suất này bắt đầu trước lúc suất kia kết thúc và ngược lại.
     * Phim chưa có thời lượng thì tính là 120 phút. Trả về tên các phim bị trùng.
     */
    public List<String> findOverlaps(int roomId, LocalDateTime start, LocalDateTime end, int ignoreShowtimeId) {
        String sql = "SELECT CONCAT(m.title, ' (', DATE_FORMAT(s.start_time, '%H:%i'), ')') "
                + "FROM showtimes s "
                + "JOIN movies m ON m.id = s.movie_id "
                + "WHERE s.room_id = ? AND s.id <> ? "
                + "  AND s.start_time < ? "
                + "  AND s.start_time + INTERVAL COALESCE(m.duration_min, 120) MINUTE > ?";
        return jdbc.queryForList(sql, String.class, roomId, ignoreShowtimeId, end, start);
    }

    public void insert(int movieId, int roomId, LocalDateTime startTime, int price) {
        String sql = "INSERT INTO showtimes (movie_id, room_id, start_time, price) VALUES (?, ?, ?, ?)";
        jdbc.update(sql, movieId, roomId, startTime, price);
    }

    public void update(int id, int movieId, int roomId, LocalDateTime startTime, int price) {
        String sql = "UPDATE showtimes SET movie_id = ?, room_id = ?, start_time = ?, price = ? WHERE id = ?";
        jdbc.update(sql, movieId, roomId, startTime, price, id);
    }

    /** Xóa suất chiếu. MySQL từ chối (lỗi khóa ngoại) nếu suất đã có người đặt vé. */
    public void delete(int id) {
        jdbc.update("DELETE FROM showtimes WHERE id = ?", id);
    }
}
