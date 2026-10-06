package vn.scarlet.cinema.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import vn.scarlet.cinema.model.SessionUser;

/**
 * Chặn các trang quản trị (/quan-tri/...).
 * Chưa đăng nhập thì chuyển sang trang đăng nhập; đã đăng nhập nhưng không phải quản trị viên thì báo lỗi 403.
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        SessionUser user = Sessions.currentUser(request.getSession());
        if (user == null) {
            String target = URLEncoder.encode(request.getRequestURI(), StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/dang-nhap?tiep=" + target);
            return false;
        }
        if (!user.admin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}
