package vn.scarlet.cinema.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import vn.scarlet.cinema.model.Room;

/** Các câu lệnh SQL trên bảng rooms. */
@Repository
public class RoomDao {

    private static final RowMapper<Room> MAPPER = (rs, rowNum) -> new Room(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getInt("seat_rows"),
            rs.getInt("seats_per_row"));

    private final JdbcTemplate jdbc;

    public RoomDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Room> findAll() {
        return jdbc.query("SELECT id, name, seat_rows, seats_per_row FROM rooms ORDER BY id", MAPPER);
    }

    public Optional<Room> findById(int id) {
        String sql = "SELECT id, name, seat_rows, seats_per_row FROM rooms WHERE id = ?";
        return jdbc.query(sql, MAPPER, id).stream().findFirst();
    }
}
