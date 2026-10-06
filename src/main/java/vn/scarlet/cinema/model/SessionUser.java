package vn.scarlet.cinema.model;

import java.io.Serializable;

/**
 * Thông tin người đang đăng nhập, lưu trong phiên (HttpSession).
 * Chỉ giữ mã, tên và vai trò; không giữ mật khẩu.
 */
public record SessionUser(int id, String fullName, String role) implements Serializable {

    public boolean admin() {
        return "ADMIN".equals(role);
    }

    /** Tên gọi ngắn để chào trên thanh điều hướng: "Nguyễn Minh Anh" thành "Minh Anh". */
    public String shortName() {
        String[] words = fullName.trim().split("\\s+");
        if (words.length <= 2) {
            return fullName.trim();
        }
        return words[words.length - 2] + " " + words[words.length - 1];
    }
}
