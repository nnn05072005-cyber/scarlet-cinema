package vn.scarlet.cinema.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.scarlet.cinema.dao.MovieDao;
import vn.scarlet.cinema.dao.RoomDao;
import vn.scarlet.cinema.dao.ShowtimeDao;
import vn.scarlet.cinema.model.Movie;
import vn.scarlet.cinema.model.Room;
import vn.scarlet.cinema.model.ShowtimeView;

/** Quản trị: xem suất chiếu theo ngày, thêm, sửa, xóa suất chiếu. */
@Controller
public class AdminShowtimeController {

    private final ShowtimeDao showtimeDao;
    private final MovieDao movieDao;
    private final RoomDao roomDao;

    public AdminShowtimeController(ShowtimeDao showtimeDao, MovieDao movieDao, RoomDao roomDao) {
        this.showtimeDao = showtimeDao;
        this.movieDao = movieDao;
        this.roomDao = roomDao;
    }

    /** Danh sách suất chiếu của một ngày (?ngay=...) và mẫu nhập. Có ?sua=<mã suất> thì mẫu nhập hiện sẵn suất đó. */
    @GetMapping("/quan-tri/suat-chieu")
    public String list(@RequestParam(name = "ngay", required = false) String dayParam,
                       @RequestParam(name = "sua", required = false) Integer editId,
                       Model model) {
        LocalDate today = LocalDate.now();
        Optional<ShowtimeView> editing = editId == null ? Optional.empty() : showtimeDao.findById(editId);

        LocalDate day = MovieController.parseDay(dayParam);
        if (day == null) {
            day = editing.map(s -> s.startTime().toLocalDate()).orElse(today);
        }
        List<LocalDate> days = new ArrayList<>();
        for (int i = 0; i < MovieController.DAYS; i++) {
            days.add(today.plusDays(i));
        }

        List<Room> rooms = roomDao.findAll();
        model.addAttribute("cacNgay", days);
        model.addAttribute("ngayChon", day);
        model.addAttribute("dsSuat", showtimeDao.findBetween(day.atStartOfDay(), day.plusDays(1).atStartOfDay()));
        model.addAttribute("dsPhim", movieDao.findSchedulable());
        model.addAttribute("dsPhong", rooms);
        model.addAttribute("soPhong", rooms.size());

        if (!model.containsAttribute("form")) {
            Map<String, String> form = emptyForm(day);
            editing.ifPresent(s -> {
                form.put("id", String.valueOf(s.id()));
                form.put("phim", String.valueOf(s.movieId()));
                form.put("phong", String.valueOf(s.roomId()));
                form.put("ngay", s.startTime().toLocalDate().toString());
                form.put("gio", s.startTime().toLocalTime().toString().substring(0, 5));
                form.put("gia", String.valueOf(s.price()));
            });
            model.addAttribute("form", form);
        }
        return "quan-tri/suat-chieu";
    }

    /** Lưu suất chiếu: không có mã thì thêm mới, có mã thì cập nhật. */
    @PostMapping("/quan-tri/suat-chieu/luu")
    public String save(@RequestParam Map<String, String> params, RedirectAttributes redirect) {
        Map<String, String> form = emptyForm(LocalDate.now());
        form.replaceAll((key, old) -> params.getOrDefault(key, "").trim());
        Integer id = form.get("id").isEmpty() ? null : AdminMovieController.parseInt(form.get("id"));

        String error = null;
        Integer movieId = AdminMovieController.parseInt(form.get("phim"));
        Integer roomId = AdminMovieController.parseInt(form.get("phong"));
        Integer price = AdminMovieController.parseInt(form.get("gia"));
        LocalDate day = MovieController.parseDay(form.get("ngay"));
        LocalTime time = parseTime(form.get("gio"));
        Optional<Movie> movie = movieId == null ? Optional.empty() : movieDao.findById(movieId);
        Optional<Room> room = roomId == null ? Optional.empty() : roomDao.findById(roomId);
        Optional<ShowtimeView> current = id == null ? Optional.empty() : showtimeDao.findById(id);

        if (!form.get("id").isEmpty() && current.isEmpty()) {
            error = "Không tìm thấy suất chiếu cần sửa.";
        } else if (current.isPresent() && current.get().sold() > 0) {
            error = "Suất chiếu này đã có người đặt vé nên không sửa được.";
        } else if (movie.isEmpty() || "ENDED".equals(movie.get().status())) {
            error = "Bạn chưa chọn phim.";
        } else if (room.isEmpty()) {
            error = "Bạn chưa chọn phòng.";
        } else if (day == null) {
            error = "Bạn chưa chọn ngày chiếu.";
        } else if (time == null) {
            error = "Giờ chiếu phải có dạng giờ:phút, ví dụ 19:30.";
        } else if (!LocalDateTime.of(day, time).isAfter(LocalDateTime.now())) {
            error = "Giờ chiếu phải sau thời điểm hiện tại.";
        } else if (price == null || price < 10000 || price > 1000000) {
            error = "Giá vé là số tiền từ 10.000 đến 1.000.000 đồng.";
        } else {
            // Kiểm tra trùng giờ với suất khác trong cùng phòng
            LocalDateTime start = LocalDateTime.of(day, time);
            int minutes = movie.get().durationMin() == null ? 120 : movie.get().durationMin();
            List<String> overlaps = showtimeDao.findOverlaps(roomId, start, start.plusMinutes(minutes), id == null ? 0 : id);
            if (!overlaps.isEmpty()) {
                error = room.get().name() + " bị trùng giờ với suất: " + String.join(", ", overlaps) + ".";
            }
        }

        if (error == null) {
            LocalDateTime start = LocalDateTime.of(day, time);
            try {
                if (id == null) {
                    showtimeDao.insert(movieId, roomId, start, price);
                    redirect.addFlashAttribute("thongBao", "Đã thêm suất chiếu " + form.get("gio") + " của phim \"" + movie.get().title() + "\".");
                } else {
                    showtimeDao.update(id, movieId, roomId, start, price);
                    redirect.addFlashAttribute("thongBao", "Đã lưu thay đổi của suất chiếu.");
                }
                return "redirect:/quan-tri/suat-chieu?ngay=" + day;
            } catch (DataIntegrityViolationException e) {
                // Ràng buộc UNIQUE (room_id, start_time): phòng đã có suất bắt đầu đúng giờ này
                error = "Phòng này đã có suất chiếu bắt đầu đúng giờ đó.";
            }
        }

        redirect.addFlashAttribute("loi", error);
        redirect.addFlashAttribute("form", form);
        String query = id != null ? "?sua=" + id : day != null ? "?ngay=" + day : "";
        return "redirect:/quan-tri/suat-chieu" + query;
    }

    @PostMapping("/quan-tri/suat-chieu/{id}/xoa")
    public String delete(@PathVariable("id") int id,
                         @RequestParam(name = "ngay", defaultValue = "") String dayParam,
                         RedirectAttributes redirect) {
        try {
            showtimeDao.delete(id);
            redirect.addFlashAttribute("thongBao", "Đã xóa suất chiếu.");
        } catch (DataIntegrityViolationException e) {
            // Khóa ngoại fk_bookings_showtime: suất đã có đơn đặt vé thì MySQL không cho xóa
            redirect.addFlashAttribute("loi", "Suất chiếu này đã có người đặt vé nên không xóa được.");
        }
        LocalDate day = MovieController.parseDay(dayParam);
        return "redirect:/quan-tri/suat-chieu" + (day == null ? "" : "?ngay=" + day);
    }

    private Map<String, String> emptyForm(LocalDate day) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("id", "");
        form.put("phim", "");
        form.put("phong", "");
        form.put("ngay", day.toString());
        form.put("gio", "");
        form.put("gia", "75000");
        return form;
    }

    private static LocalTime parseTime(String text) {
        try {
            return LocalTime.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
