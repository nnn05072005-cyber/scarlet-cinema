package vn.scarlet.cinema.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import vn.scarlet.cinema.dao.MovieDao;
import vn.scarlet.cinema.dao.ShowtimeDao;
import vn.scarlet.cinema.model.Movie;
import vn.scarlet.cinema.model.ShowtimeView;

/** Trang chi tiết phim và lịch chiếu 7 ngày tới của phim đó. */
@Controller
public class MovieController {

    /** Số ngày hiện trong ô chọn ngày. */
    static final int DAYS = 7;

    private final MovieDao movieDao;
    private final ShowtimeDao showtimeDao;

    public MovieController(MovieDao movieDao, ShowtimeDao showtimeDao) {
        this.movieDao = movieDao;
        this.showtimeDao = showtimeDao;
    }

    @GetMapping("/phim/{id}")
    public String detail(@PathVariable("id") int id,
                         @RequestParam(name = "ngay", required = false) String dayParam,
                         Model model) {
        Movie movie = movieDao.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // Các suất còn chưa chiếu của phim trong 7 ngày tới
        List<ShowtimeView> week = showtimeDao.findByMovieBetween(id, now, today.plusDays(DAYS).atStartOfDay());

        List<LocalDate> days = new ArrayList<>();
        for (int i = 0; i < DAYS; i++) {
            days.add(today.plusDays(i));
        }

        // Ngày đang chọn: lấy từ địa chỉ (?ngay=...), nếu không có thì chọn ngày đầu tiên còn suất chiếu
        LocalDate selected = parseDay(dayParam);
        if (selected == null || !days.contains(selected)) {
            selected = week.isEmpty() ? today : week.get(0).startTime().toLocalDate();
        }
        final LocalDate selectedDay = selected;
        List<ShowtimeView> showtimes = week.stream()
                .filter(s -> s.startTime().toLocalDate().equals(selectedDay))
                .toList();

        model.addAttribute("phim", movie);
        model.addAttribute("cacNgay", days);
        model.addAttribute("ngayChon", selectedDay);
        model.addAttribute("cacSuat", showtimes);
        model.addAttribute("coLich", !week.isEmpty());
        return "phim";
    }

    static LocalDate parseDay(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
