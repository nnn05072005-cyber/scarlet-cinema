package vn.scarlet.cinema.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Chặn các trang cần đăng nhập (chọn ghế, thanh toán, vé, lịch sử vé).
 * Chưa đăng nhập thì chuyển sang trang đăng nhập, kèm địa chỉ đang muốn vào để quay lại sau.
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (Sessions.currentUser(request.getSession()) != null) {
            return true;
        }
        String target = request.getRequestURI();
        if ("GET".equals(request.getMethod()) && request.getQueryString() != null) {
            target += "?" + request.getQueryString();
        }
        response.sendRedirect(request.getContextPath() + "/dang-nhap?tiep=" + URLEncoder.encode(target, StandardCharsets.UTF_8));
        return false;
    }
}
