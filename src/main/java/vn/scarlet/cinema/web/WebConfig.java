package vn.scarlet.cinema.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Khai báo trang nào cần đăng nhập, trang nào chỉ dành cho quản trị viên. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final AdminInterceptor adminInterceptor;

    public WebConfig(LoginInterceptor loginInterceptor, AdminInterceptor adminInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor).addPathPatterns("/dat-ve", "/dat-ve/**", "/thanh-toan/**", "/ve/**", "/ve-cua-toi");
        registry.addInterceptor(adminInterceptor).addPathPatterns("/quan-tri", "/quan-tri/**");
    }
}
