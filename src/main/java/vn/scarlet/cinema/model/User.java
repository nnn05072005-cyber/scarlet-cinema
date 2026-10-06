package vn.scarlet.cinema.model;

/** Một dòng của bảng users. */
public record User(int id, String fullName, String email, String passwordHash, String role) {

    public boolean admin() {
        return "ADMIN".equals(role);
    }
}
