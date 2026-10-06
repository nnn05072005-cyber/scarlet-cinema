package vn.scarlet.cinema.web;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.scarlet.cinema.dao.MovieDao;
import vn.scarlet.cinema.model.Movie;

/** Quản trị: xem danh sách, thêm, sửa, xóa phim. Chỉ quản trị viên vào được (xem WebConfig). */
@Controller
public class AdminMovieController {

    private static final Set<String> RATINGS = Set.of("P", "K", "T13", "T16", "T18");
    private static final Set<String> STATUSES = Set.of("NOW_SHOWING", "COMING_SOON", "ENDED");

    private final MovieDao movieDao;

    public AdminMovieController(MovieDao movieDao) {
        this.movieDao = movieDao;
    }

    @GetMapping("/quan-tri")
    public String index() {
        return "redirect:/quan-tri/phim";
    }

    /** Danh sách phim và mẫu nhập. Có ?sua=<mã phim> thì mẫu nhập hiện sẵn thông tin phim đó để sửa. */
    @GetMapping("/quan-tri/phim")
    public String list(@RequestParam(name = "sua", required = false) Integer editId, Model model) {
        List<Movie> movies = movieDao.findAll();
        model.addAttribute("dsPhim", movies);
        model.addAttribute("soDangChieu", movies.stream().filter(Movie::nowShowing).count());
        model.addAttribute("soSapChieu", movies.stream().filter(Movie::comingSoon).count());

        // Nếu vừa lưu bị lỗi thì "form" đã có sẵn (giữ lại những gì người dùng nhập); nếu không thì dựng mới
        if (!model.containsAttribute("form")) {
            Optional<Movie> editing = editId == null ? Optional.empty() : movieDao.findById(editId);
            model.addAttribute("form", editing.map(this::toForm).orElseGet(this::emptyForm));
        }
        return "quan-tri/phim";
    }

    /** Lưu phim: không có mã thì thêm mới, có mã thì cập nhật. */
    @PostMapping("/quan-tri/phim/luu")
    public String save(@RequestParam Map<String, String> params, RedirectAttributes redirect) {
        Map<String, String> form = emptyForm();
        form.replaceAll((key, old) -> params.getOrDefault(key, "").trim());

        Integer id = form.get("id").isEmpty() ? null : parseInt(form.get("id"));
        String error = validate(form);
        if (error != null) {
            redirect.addFlashAttribute("loi", error);
            redirect.addFlashAttribute("form", form);
            return "redirect:/quan-tri/phim" + (id == null ? "" : "?sua=" + id) + "#them-phim";
        }

        Movie movie = new Movie(
                id == null ? 0 : id,
                form.get("title"),
                emptyToNull(form.get("originalTitle")),
                form.get("genre"),
                form.get("durationMin").isEmpty() ? null : parseInt(form.get("durationMin")),
                emptyToNull(form.get("ageRating")),
                LocalDate.parse(form.get("releaseDate")),
                emptyToNull(form.get("director")),
                emptyToNull(form.get("description")),
                emptyToNull(form.get("posterUrl")),
                emptyToNull(form.get("backdropUrl")),
                emptyToNull(form.get("trailerUrl")),
                form.get("status"));
        if (id == null) {
            movieDao.insert(movie);
            redirect.addFlashAttribute("thongBao", "Đã thêm phim \"" + movie.title() + "\".");
        } else {
            movieDao.update(movie);
            redirect.addFlashAttribute("thongBao", "Đã lưu thay đổi của phim \"" + movie.title() + "\".");
        }
        return "redirect:/quan-tri/phim";
    }

    @PostMapping("/quan-tri/phim/{id}/xoa")
    public String delete(@PathVariable("id") int id, RedirectAttributes redirect) {
        try {
            movieDao.delete(id);
            redirect.addFlashAttribute("thongBao", "Đã xóa phim.");
        } catch (DataIntegrityViolationException e) {
            // Khóa ngoại fk_showtimes_movie: phim còn suất chiếu thì MySQL không cho xóa
            redirect.addFlashAttribute("loi", "Phim này còn suất chiếu nên không xóa được. Bạn có thể sửa trạng thái thành \"Ngừng chiếu\".");
        }
        return "redirect:/quan-tri/phim";
    }

    /** Kiểm tra dữ liệu nhập. Trả về câu báo lỗi, hoặc null nếu hợp lệ. */
    private String validate(Map<String, String> form) {
        if (!form.get("id").isEmpty() && (parseInt(form.get("id")) == null || movieDao.findById(parseInt(form.get("id"))).isEmpty())) {
            return "Không tìm thấy phim cần sửa.";
        }
        if (form.get("title").isEmpty() || form.get("title").length() > 200) {
            return "Tên phim không được để trống và dài tối đa 200 ký tự.";
        }
        if (form.get("originalTitle").length() > 200) {
            return "Tên gốc dài tối đa 200 ký tự.";
        }
        if (form.get("genre").isEmpty() || form.get("genre").length() > 100) {
            return "Thể loại không được để trống và dài tối đa 100 ký tự.";
        }
        if (!form.get("durationMin").isEmpty()) {
            Integer minutes = parseInt(form.get("durationMin"));
            if (minutes == null || minutes < 1 || minutes > 600) {
                return "Thời lượng là số phút, từ 1 đến 600. Chưa biết thì để trống.";
            }
        }
        if (!form.get("ageRating").isEmpty() && !RATINGS.contains(form.get("ageRating"))) {
            return "Độ tuổi phải là một trong: P, K, T13, T16, T18.";
        }
        if (MovieController.parseDay(form.get("releaseDate")) == null) {
            return "Bạn chưa chọn ngày khởi chiếu.";
        }
        if (form.get("director").length() > 150) {
            return "Tên đạo diễn dài tối đa 150 ký tự.";
        }
        for (String key : List.of("posterUrl", "backdropUrl")) {
            String url = form.get(key);
            if (!url.isEmpty() && (url.length() > 500 || !(url.startsWith("https://") || url.startsWith("http://") || url.startsWith("/")))) {
                return "Đường dẫn ảnh phải bắt đầu bằng https:// và dài tối đa 500 ký tự.";
            }
        }
        String trailer = form.get("trailerUrl");
        if (!trailer.isEmpty() && (trailer.length() > 500 || Movie.youtubeIdOf(trailer) == null)) {
            return "Đường dẫn trailer phải là đường dẫn một video YouTube, ví dụ https://www.youtube.com/watch?v=... Chưa có trailer thì để trống.";
        }
        if (!STATUSES.contains(form.get("status"))) {
            return "Trạng thái không hợp lệ.";
        }
        return null;
    }

    private Map<String, String> emptyForm() {
        Map<String, String> form = new LinkedHashMap<>();
        for (String key : List.of("id", "title", "originalTitle", "genre", "durationMin", "ageRating", "releaseDate",
                "director", "posterUrl", "backdropUrl", "trailerUrl", "description")) {
            form.put(key, "");
        }
        form.put("status", "COMING_SOON");
        return form;
    }

    private Map<String, String> toForm(Movie m) {
        Map<String, String> form = emptyForm();
        form.put("id", String.valueOf(m.id()));
        form.put("title", m.title());
        form.put("originalTitle", nullToEmpty(m.originalTitle()));
        form.put("genre", m.genre());
        form.put("durationMin", m.durationMin() == null ? "" : String.valueOf(m.durationMin()));
        form.put("ageRating", nullToEmpty(m.ageRating()));
        form.put("releaseDate", m.releaseDate().toString());
        form.put("director", nullToEmpty(m.director()));
        form.put("posterUrl", nullToEmpty(m.posterUrl()));
        form.put("backdropUrl", nullToEmpty(m.backdropUrl()));
        form.put("trailerUrl", nullToEmpty(m.trailerUrl()));
        form.put("description", nullToEmpty(m.description()));
        form.put("status", m.status());
        return form;
    }

    static Integer parseInt(String text) {
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String emptyToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}
