package vn.scarlet.cinema.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import vn.scarlet.cinema.dao.RoomDao;
import vn.scarlet.cinema.model.MovieText;
import vn.scarlet.cinema.model.Room;
import vn.scarlet.cinema.service.BookingService;

/** Trang thông tin: giới thiệu rạp, phòng chiếu, quy định đặt vé, thanh toán, phân loại độ tuổi, hỏi đáp, bảo mật. */
@Controller
public class InfoController {

    /** Các mức phân loại độ tuổi theo thứ tự từ rộng đến hẹp. */
    private static final List<String> RATINGS = List.of("P", "K", "T13", "T16", "T18");

    private final RoomDao roomDao;

    public InfoController(RoomDao roomDao) {
        this.roomDao = roomDao;
    }

    @GetMapping("/thong-tin")
    public String info(Model model) {
        // Phòng chiếu lấy từ bảng rooms, nên trang luôn khớp với dữ liệu thật
        List<Room> rooms = roomDao.findAll();
        int totalSeats = 0;
        for (Room room : rooms) {
            totalSeats += room.totalSeats();
        }
        model.addAttribute("dsPhong", rooms);
        model.addAttribute("tongGhe", totalSeats);

        // Hai con số của quy định đặt vé lấy thẳng từ nơi xử lý đặt vé, không ghi cứng trong trang
        model.addAttribute("toiDa", BookingService.MAX_SEATS);
        model.addAttribute("phutGiu", BookingService.HOLD_MINUTES);

        Map<String, String> ratings = new LinkedHashMap<>();
        for (String code : RATINGS) {
            ratings.put(code, MovieText.note(code));
        }
        model.addAttribute("doTuoi", ratings);
        return "thong-tin";
    }
}
