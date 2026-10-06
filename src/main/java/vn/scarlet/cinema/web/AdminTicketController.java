package vn.scarlet.cinema.web;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import vn.scarlet.cinema.dao.BookingDao;
import vn.scarlet.cinema.dao.PromotionDao;

/** Quản trị: danh sách vé đã đặt (tìm theo mã vé hoặc tên khách), doanh thu theo phim và tình hình dùng mã ưu đãi. */
@Controller
public class AdminTicketController {

    /** Số đơn hiện trên một trang. */
    private static final int LIMIT = 50;

    private final BookingDao bookingDao;
    private final PromotionDao promotionDao;

    public AdminTicketController(BookingDao bookingDao, PromotionDao promotionDao) {
        this.bookingDao = bookingDao;
        this.promotionDao = promotionDao;
    }

    @GetMapping("/quan-tri/ve")
    public String list(@RequestParam(name = "tim", defaultValue = "") String keyword, Model model) {
        String cleanKeyword = keyword.trim();
        Map<String, Object> totals = bookingDao.totals();
        model.addAttribute("tim", cleanKeyword);
        model.addAttribute("dsVe", bookingDao.search(cleanKeyword, LIMIT));
        model.addAttribute("gioiHan", LIMIT);
        model.addAttribute("tongDon", ((Number) totals.get("bookings")).longValue());
        model.addAttribute("tongVe", ((Number) totals.get("tickets")).longValue());
        model.addAttribute("tongTien", ((Number) totals.get("revenue")).longValue());
        model.addAttribute("tongGiam", ((Number) totals.get("discount")).longValue());
        model.addAttribute("doanhThu", bookingDao.revenueByMovie());
        model.addAttribute("dsMa", promotionDao.stats());
        model.addAttribute("homNay", LocalDate.now());
        return "quan-tri/ve";
    }
}
