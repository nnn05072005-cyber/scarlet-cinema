package vn.scarlet.cinema.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import vn.scarlet.cinema.model.User;

/** Các câu lệnh SQL trên bảng users. */
@Repository
public class UserDao {

    private static final RowMapper<User> MAPPER = (rs, rowNum) -> new User(
            rs.getInt("id"),
            rs.getString("full_name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("role"));

    private final JdbcTemplate jdbc;

    public UserDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Tìm tài khoản theo email (dùng khi đăng nhập và khi kiểm tra email đã có chưa). */
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, full_name, email, password_hash, role FROM users WHERE email = ?";
        return jdbc.query(sql, MAPPER, email).stream().findFirst();
    }

    /** Thêm tài khoản khách mới, trả về mã tài khoản vừa tạo. */
    public int insertCustomer(String fullName, String email, String passwordHash) {
        String sql = "INSERT INTO users (full_name, email, password_hash, role) VALUES (?, ?, ?, 'CUSTOMER')";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().intValue();
    }
}
