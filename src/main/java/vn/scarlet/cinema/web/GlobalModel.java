package vn.scarlet.cinema.web;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import vn.scarlet.cinema.model.SessionUser;

/** Đưa thông tin người đang đăng nhập vào mọi trang, để thanh điều hướng hiện đúng tên và các liên kết. */
@ControllerAdvice
public class GlobalModel {

    @ModelAttribute(Sessions.USER)
    public SessionUser currentUser(HttpSession session) {
        return Sessions.currentUser(session);
    }
}
