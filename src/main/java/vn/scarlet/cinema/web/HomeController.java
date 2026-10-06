package vn.scarlet.cinema.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import vn.scarlet.cinema.dao.MovieDao;
import vn.scarlet.cinema.dao.PromotionDao;
import vn.scarlet.cinema.dao.ShowtimeDao;
import vn.scarlet.cinema.model.Movie;
import vn.scarlet.cinema.model.ShowtimeView;

/** Trang chủ (phim nổi bật, suất chiếu sắp tới, đặt vé nhanh, phim đang chiếu, phim sắp chiếu, ưu đãi) và trang phim sắp chiếu. */
@Controller
public class HomeController {

    private final MovieDao movieDao;
    private final ShowtimeDao showtimeDao;
    private final PromotionDao promotionDao;
    private final Fmt fmt;

    public HomeController(MovieDao movieDao, ShowtimeDao showtimeDao, PromotionDao promotionDao, Fmt fmt) {
        this.movieDao = movieDao;
        this.showtimeDao = showtimeDao;
        this.promotionDao = promotionDao;
        this.fmt = fmt;
    }

    @GetMapping("/")
    public String home(@RequestParam(name = "theLoai", defaultValue = "") String genre,
                       @RequestParam(name = "tim", defaultValue = "") String keyword,
                       Model model) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 1. Phim nổi bật
        model.addAttribute("noiBat", movieDao.findFeatured().orElse(null));

        // 2. Khối "Hôm nay": các suất sắp chiếu của ngày gần nhất còn suất
        LocalDateTime next = showtimeDao.findNextStartTime(now);
        LocalDate day = next == null ? today : next.toLocalDate();
        List<ShowtimeView> upcoming = next == null ? List.of()
                : showtimeDao.findBetween(next, day.plusDays(1).atStartOfDay()).stream().limit(5).toList();
        model.addAttribute("suatSapToi", upcoming);
        model.addAttribute("ngaySuat", day);
        model.addAttribute("tenNgaySuat", day.equals(today) ? "Hôm nay" : day.equals(today.plusDays(1)) ? "Ngày mai" : fmt.thu(day));

        // 3. Phim đang chiếu, có lọc theo thể loại và tìm theo tên
        List<Movie> allNowShowing = movieDao.findNowShowing("", "");
        String cleanGenre = genre.trim();
        String cleanKeyword = keyword.trim();
        boolean filtering = !cleanGenre.isEmpty() || !cleanKeyword.isEmpty();
        model.addAttribute("dangChieu", filtering ? movieDao.findNowShowing(cleanGenre, cleanKeyword) : allNowShowing);
        model.addAttribute("tongDangChieu", allNowShowing.size());
        model.addAttribute("dsTheLoai", topGenres(allNowShowing, 4));
        model.addAttribute("theLoai", cleanGenre);
        model.addAttribute("tim", cleanKeyword);

        // 4. Phim sắp chiếu
        model.addAttribute("sapChieu", movieDao.findComingSoon());

        // 5. Ưu đãi: các mã đang dùng được (nhiều nhất 4 mã), lấy từ bảng promotions
        model.addAttribute("uuDai", promotionDao.findUsable(4));

        // 6. Đặt vé nhanh: các suất chiếu trong 7 ngày tới, đưa sang JavaScript để dựng ba ô chọn
        List<Map<String, Object>> quick = new ArrayList<>();
        for (ShowtimeView s : showtimeDao.findBetween(now, today.plusDays(7).atStartOfDay())) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.id());
            item.put("phimId", s.movieId());
            item.put("phim", s.movieTitle());
            item.put("ngay", s.startTime().toLocalDate().toString());
            item.put("nhanNgay", fmt.thu(s.startTime()) + ", " + fmt.ngay(s.startTime()));
            item.put("gio", fmt.gio(s.startTime()));
            item.put("phong", s.roomName());
            quick.add(item);
        }
        model.addAttribute("suatNhanh", quick);
        return "index";
    }

    /** Trang liệt kê toàn bộ phim sắp chiếu, xếp theo ngày khởi chiếu. Nút "Xem thêm" ở trang chủ dẫn tới đây. */
    @GetMapping("/phim-sap-chieu")
    public String comingSoon(Model model) {
        model.addAttribute("sapChieu", movieDao.findComingSoon());
        return "sap-chieu";
    }

    /** Các thể loại xuất hiện nhiều nhất trong danh sách phim, dùng làm nút lọc. */
    private List<String> topGenres(List<Movie> movies, int limit) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Movie movie : movies) {
            for (String part : movie.genre().split(",")) {
                String name = part.trim();
                if (name.isEmpty()) {
                    continue;
                }
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                counts.merge(name, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }
}
