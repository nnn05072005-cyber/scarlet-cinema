package vn.scarlet.cinema.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import vn.scarlet.cinema.model.Movie;

/** Các câu lệnh SQL trên bảng movies. */
@Repository
public class MovieDao {

    private static final String COLUMNS = "id, title, original_title, genre, duration_min, age_rating, release_date, "
            + "director, description, poster_url, backdrop_url, trailer_url, status";

    private static final RowMapper<Movie> MAPPER = (rs, rowNum) -> new Movie(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("original_title"),
            rs.getString("genre"),
            intOrNull(rs, "duration_min"),
            rs.getString("age_rating"),
            rs.getObject("release_date", LocalDate.class),
            rs.getString("director"),
            rs.getString("description"),
            rs.getString("poster_url"),
            rs.getString("backdrop_url"),
            rs.getString("trailer_url"),
            rs.getString("status"));

    private final JdbcTemplate jdbc;

    public MovieDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Cột số có thể để trống: trả về null thay vì 0. */
    static Integer intOrNull(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * Phim đang chiếu, phim mới ra xếp trước. Có thể lọc theo thể loại và tìm theo tên;
     * để chuỗi rỗng nếu không lọc.
     */
    public List<Movie> findNowShowing(String genre, String keyword) {
        String sql = "SELECT " + COLUMNS + " FROM movies "
                + "WHERE status = 'NOW_SHOWING' "
                + "  AND genre LIKE CONCAT('%', ?, '%') "
                + "  AND title LIKE CONCAT('%', ?, '%') "
                + "ORDER BY release_date DESC, id";
        return jdbc.query(sql, MAPPER, genre, keyword);
    }

    /** Phim sắp chiếu, xếp theo ngày khởi chiếu. */
    public List<Movie> findComingSoon() {
        String sql = "SELECT " + COLUMNS + " FROM movies WHERE status = 'COMING_SOON' ORDER BY release_date, id";
        return jdbc.query(sql, MAPPER);
    }

    /** Phim nổi bật ở đầu trang chủ: phim đang chiếu có nhiều suất chiếu sắp tới nhất. */
    public Optional<Movie> findFeatured() {
        String sql = "SELECT " + COLUMNS + " FROM movies m "
                + "WHERE m.status = 'NOW_SHOWING' "
                + "ORDER BY (SELECT COUNT(*) FROM showtimes s WHERE s.movie_id = m.id AND s.start_time > NOW()) DESC, m.id "
                + "LIMIT 1";
        return jdbc.query(sql, MAPPER).stream().findFirst();
    }

    /** Toàn bộ phim cho trang quản trị: đang chiếu trước, rồi sắp chiếu, rồi ngừng chiếu. */
    public List<Movie> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM movies "
                + "ORDER BY FIELD(status, 'NOW_SHOWING', 'COMING_SOON', 'ENDED'), release_date DESC, id";
        return jdbc.query(sql, MAPPER);
    }

    /** Phim có thể xếp suất chiếu: đang chiếu và sắp chiếu. */
    public List<Movie> findSchedulable() {
        String sql = "SELECT " + COLUMNS + " FROM movies WHERE status <> 'ENDED' ORDER BY status, title";
        return jdbc.query(sql, MAPPER);
    }

    public Optional<Movie> findById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM movies WHERE id = ?";
        return jdbc.query(sql, MAPPER, id).stream().findFirst();
    }

    public void insert(Movie m) {
        String sql = "INSERT INTO movies (title, original_title, genre, duration_min, age_rating, release_date, "
                + "director, description, poster_url, backdrop_url, trailer_url, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbc.update(sql, m.title(), m.originalTitle(), m.genre(), m.durationMin(), m.ageRating(), m.releaseDate(),
                m.director(), m.description(), m.posterUrl(), m.backdropUrl(), m.trailerUrl(), m.status());
    }

    public void update(Movie m) {
        String sql = "UPDATE movies SET title = ?, original_title = ?, genre = ?, duration_min = ?, age_rating = ?, "
                + "release_date = ?, director = ?, description = ?, poster_url = ?, backdrop_url = ?, trailer_url = ?, status = ? "
                + "WHERE id = ?";
        jdbc.update(sql, m.title(), m.originalTitle(), m.genre(), m.durationMin(), m.ageRating(), m.releaseDate(),
                m.director(), m.description(), m.posterUrl(), m.backdropUrl(), m.trailerUrl(), m.status(), m.id());
    }

    /** Xóa phim. MySQL từ chối (lỗi khóa ngoại) nếu phim còn suất chiếu. */
    public void delete(int id) {
        jdbc.update("DELETE FROM movies WHERE id = ?", id);
    }
}
