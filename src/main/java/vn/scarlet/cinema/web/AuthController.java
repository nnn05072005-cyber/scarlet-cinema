package vn.scarlet.cinema.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import vn.scarlet.cinema.dao.PromotionDao;
import vn.scarlet.cinema.dao.UserDao;
import vn.scarlet.cinema.model.Promotion;
import vn.scarlet.cinema.model.SessionUser;
import vn.scarlet.cinema.model.User;

/** Đăng nhập, đăng ký, đăng xuất. */
@Controller
public class AuthController {

    /** BCrypt băm mật khẩu một chiều: trong bảng users chỉ lưu chuỗi đã băm, không lưu mật khẩu gốc. */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserDao userDao;
    private final PromotionDao promotionDao;

    public AuthController(UserDao userDao, PromotionDao promotionDao) {
        this.userDao = userDao;
        this.promotionDao = promotionDao;
    }

    /** Hai mã ưu đãi đang dùng được, để in phiếu ưu đãi trên trang đăng nhập và trang đăng ký. */
    @ModelAttribute("uuDai")
    public List<Promotion> promotions() {
        return promotionDao.findUsable(2);
    }

    @GetMapping("/dang-nhap")
    public String loginForm(@RequestParam(name = "tiep", defaultValue = "") String next, HttpSession session, Model model) {
        if (Sessions.currentUser(session) != null) {
            return "redirect:/";
        }
        model.addAttribute("tiep", next);
        return "dang-nhap";
    }

    @PostMapping("/dang-nhap")
    public String login(@RequestParam("email") String email,
                        @RequestParam("matKhau") String password,
                        @RequestParam(name = "tiep", defaultValue = "") String next,
                        HttpServletRequest request, Model model) {
        String cleanEmail = email.trim().toLowerCase();
        Optional<User> found = userDao.findByEmail(cleanEmail);
        // So mật khẩu người dùng nhập với chuỗi đã băm trong cơ sở dữ liệu
        if (found.isEmpty() || !passwordEncoder.matches(password, found.get().passwordHash())) {
            model.addAttribute("loi", "Email hoặc mật khẩu không đúng.");
            model.addAttribute("email", cleanEmail);
            model.addAttribute("tiep", next);
            return "dang-nhap";
        }
        User user = found.get();
        signIn(request, user.id(), user.fullName(), user.role());
        if (user.admin()) {
            return "redirect:/quan-tri/phim";
        }
        return "redirect:" + safeLocalPath(next);
    }

    @GetMapping("/dang-ky")
    public String registerForm(HttpSession session, Model model) {
        if (Sessions.currentUser(session) != null) {
            return "redirect:/";
        }
        model.addAttribute("form", new LinkedHashMap<String, String>());
        return "dang-ky";
    }

    @PostMapping("/dang-ky")
    public String register(@RequestParam("hoTen") String fullName,
                           @RequestParam("email") String email,
                           @RequestParam("matKhau") String password,
                           @RequestParam("nhapLai") String confirm,
                           HttpServletRequest request, Model model) {
        String cleanName = fullName.trim().replaceAll("\\s+", " ");
        String cleanEmail = email.trim().toLowerCase();

        String error = null;
        if (cleanName.length() < 2 || cleanName.length() > 100) {
            error = "Họ và tên cần từ 2 đến 100 ký tự.";
        } else if (cleanEmail.length() > 150 || !cleanEmail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            error = "Email không đúng dạng, ví dụ ban@example.com.";
        } else if (password.length() < 8 || password.length() > 72) {
            error = "Mật khẩu cần từ 8 đến 72 ký tự.";
        } else if (!password.equals(confirm)) {
            error = "Hai lần nhập mật khẩu không giống nhau.";
        } else if (userDao.findByEmail(cleanEmail).isPresent()) {
            error = "Email này đã có tài khoản. Bạn hãy đăng nhập.";
        }

        if (error == null) {
            try {
                int id = userDao.insertCustomer(cleanName, cleanEmail, passwordEncoder.encode(password));
                signIn(request, id, cleanName, "CUSTOMER");
                return "redirect:/";
            } catch (DuplicateKeyException e) {
                // Hai người đăng ký cùng một email cùng lúc: ràng buộc UNIQUE trên cột email chặn người sau
                error = "Email này đã có tài khoản. Bạn hãy đăng nhập.";
            }
        }

        Map<String, String> form = new LinkedHashMap<>();
        form.put("hoTen", cleanName);
        form.put("email", cleanEmail);
        model.addAttribute("form", form);
        model.addAttribute("loi", error);
        return "dang-ky";
    }

    @PostMapping("/dang-xuat")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    /** Ghi người dùng vào phiên. Đổi mã phiên khi đăng nhập để tránh bị dùng lại mã phiên cũ. */
    private void signIn(HttpServletRequest request, int id, String fullName, String role) {
        request.getSession();
        request.changeSessionId();
        request.getSession().setAttribute(Sessions.USER, new SessionUser(id, fullName, role));
    }

    /** Chỉ cho quay lại địa chỉ nằm trong chính trang web này (bắt đầu bằng một dấu /). */
    private String safeLocalPath(String path) {
        if (path == null || !path.startsWith("/") || path.startsWith("//") || path.contains("\\")) {
            return "/";
        }
        return path;
    }
}
