package vn.scarlet.cinema.web;

import jakarta.servlet.http.HttpSession;

import vn.scarlet.cinema.model.SessionUser;

/** Đọc và ghi thông tin người đăng nhập trong phiên (HttpSession). */
public final class Sessions {

    /** Tên thuộc tính trong phiên; các trang HTML cũng đọc bằng tên này. */
    public static final String USER = "nguoiDung";

    private Sessions() {
    }

    /** Người đang đăng nhập, hoặc null nếu chưa đăng nhập. */
    public static SessionUser currentUser(HttpSession session) {
        return (SessionUser) session.getAttribute(USER);
    }
}
