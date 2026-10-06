package vn.scarlet.cinema.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.servlet.http.HttpSession;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.scarlet.cinema.dao.BookingDao;
import vn.scarlet.cinema.dao.ShowtimeDao;
import vn.scarlet.cinema.model.BookingView;
import vn.scarlet.cinema.model.SessionUser;
import vn.scarlet.cinema.model.ShowtimeView;
import vn.scarlet.cinema.service.BookingException;
import vn.scarlet.cinema.service.BookingService;
import vn.scarlet.cinema.service.QrService;

/**
 * Chọn ghế, thanh toán, xem vé có mã QR, lịch sử vé. Các trang này đều cần đăng nhập (xem WebConfig).
 * Trình tự: chọn ghế, giữ ghế, thanh toán (có thể nhập mã ưu đãi), rồi mới có vé.
 */
@Controller
public class BookingController {

    /** Một ghế trên sơ đồ. */
    public record Seat(String code, int number, boolean sold) {
    }

    /** Một hàng ghế, chia hai nửa trái và phải, ở giữa là lối đi. */
    public record SeatRow(String letter, List<Seat> left, List<Seat> right) {
    }

    private final ShowtimeDao showtimeDao;
    private final BookingDao bookingDao;
    private final BookingService bookingService;
    private final QrService qrService;
    private final Fmt fmt;

    public BookingController(ShowtimeDao showtimeDao, BookingDao bookingDao, BookingService bookingService, QrService qrService, Fmt fmt) {
        this.showtimeDao = showtimeDao;
        this.bookingDao = bookingDao;
        this.bookingService = bookingService;
        this.qrService = qrService;
        this.fmt = fmt;
    }

    /** Nút "Chọn ghế" của khối đặt vé nhanh ở trang chủ gửi về đây: /dat-ve?suat=114 */
    @GetMapping("/dat-ve")
    public String quick(@RequestParam(name = "suat", required = false) Integer showtimeId) {
        return showtimeId == null ? "redirect:/" : "redirect:/dat-ve/" + showtimeId;
    }

    /** Trang chọn ghế của một suất chiếu. */
    @GetMapping("/dat-ve/{id}")
    public String seats(@PathVariable("id") int id, HttpSession session, Model model, RedirectAttributes redirect) {
        bookingService.releaseExpired();
        ShowtimeView showtime = showtimeDao.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (Sessions.currentUser(session).admin()) {
            return refuseAdmin(showtime, redirect);
        }
        if (showtime.started()) {
            redirect.addFlashAttribute("loi", "Suất chiếu này đã bắt đầu nên không đặt vé được nữa. Bạn hãy chọn suất khác.");
            return "redirect:/phim/" + showtime.movieId();
        }

        Set<String> sold = new HashSet<>(showtimeDao.findSoldSeats(id));
        int half = (showtime.seatsPerRow() + 1) / 2;
        List<SeatRow> rows = new ArrayList<>();
        for (int r = 0; r < showtime.seatRows(); r++) {
            String letter = String.valueOf((char) ('A' + r));
            List<Seat> left = new ArrayList<>();
            List<Seat> right = new ArrayList<>();
            for (int n = 1; n <= showtime.seatsPerRow(); n++) {
                String code = letter + n;
                (n <= half ? left : right).add(new Seat(code, n, sold.contains(code)));
            }
            rows.add(new SeatRow(letter, left, right));
        }

        model.addAttribute("suat", showtime);
        model.addAttribute("cacHang", rows);
        model.addAttribute("toiDa", BookingService.MAX_SEATS);
        model.addAttribute("phutGiu", BookingService.HOLD_MINUTES);
        // Nếu khách đang có đơn chờ thanh toán ở suất này thì nhắc, kèm đường dẫn sang trang thanh toán
        model.addAttribute("donCho", bookingDao.findPending(Sessions.currentUser(session).id(), id).orElse(null));
        return "chon-ghe";
    }

    /** Bấm "Tiếp tục thanh toán": giữ các ghế đã chọn trong 10 phút, rồi chuyển sang trang thanh toán. */
    @PostMapping("/dat-ve/{id}")
    public String hold(@PathVariable("id") int id,
                       @RequestParam(name = "ghe", required = false) List<String> seats,
                       HttpSession session, RedirectAttributes redirect) {
        SessionUser user = Sessions.currentUser(session);
        if (user.admin()) {
            ShowtimeView showtime = showtimeDao.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            return refuseAdmin(showtime, redirect);
        }
        // Nhả ghế của các đơn đã quá hạn trước, để những ghế đó đặt lại được ngay
        bookingService.releaseExpired();
        try {
            String code = bookingService.hold(user.id(), id, seats);
            return "redirect:/thanh-toan/" + code;
        } catch (BookingException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            // MySQL báo trùng (showtime_id, seat_code): có người vừa đặt hoặc đang giữ ghế đó. Giao dịch đã được hủy.
            redirect.addFlashAttribute("loi", "Có ghế bạn chọn vừa được người khác đặt trước. Sơ đồ ghế đã cập nhật, bạn hãy chọn lại.");
        } catch (ConcurrencyFailureException e) {
            // Nhiều người đặt cùng lúc làm MySQL phải hủy một giao dịch (deadlock). Không có gì được lưu.
            redirect.addFlashAttribute("loi", "Đang có nhiều người đặt cùng lúc. Bạn hãy bấm lại lần nữa.");
        }
        return "redirect:/dat-ve/" + id;
    }

    /**
     * Tài khoản quản trị chỉ dùng để quản lý phim, suất chiếu và vé, không đặt vé.
     * Quản trị viên bấm đặt vé thì được đưa về trang phim kèm lời giải thích.
     */
    private String refuseAdmin(ShowtimeView showtime, RedirectAttributes redirect) {
        redirect.addFlashAttribute("loi", "Tài khoản quản trị không dùng để đặt vé. Bạn hãy đăng xuất rồi đăng nhập bằng tài khoản khách để đặt vé.");
        return "redirect:/phim/" + showtime.movieId();
    }

    /** Trang thanh toán của một đơn đang giữ ghế. */
    @GetMapping("/thanh-toan/{code}")
    public String payment(@PathVariable("code") String code, HttpSession session, Model model, RedirectAttributes redirect) {
        SessionUser user = Sessions.currentUser(session);
        BookingView booking = bookingDao.findByCode(code).filter(b -> b.userId() == user.id()).orElse(null);
        if (booking == null) {
            redirect.addFlashAttribute("loi", "Đơn này không còn. Có thể đơn đã hết thời gian giữ ghế nên bị hủy.");
            return "redirect:/ve-cua-toi";
        }
        if (booking.paid()) {
            return "redirect:/ve/" + code;
        }
        if (booking.expired()) {
            bookingService.cancel(user.id(), code);
            redirect.addFlashAttribute("loi", "Đã hết " + BookingService.HOLD_MINUTES + " phút giữ ghế nên đơn bị hủy. Bạn hãy chọn ghế lại.");
            return "redirect:/dat-ve/" + booking.showtimeId();
        }
        model.addAttribute("don", booking);
        // Mã QR chuyển khoản của đơn: nội dung gồm mã đơn và số tiền
        model.addAttribute("qr", qrService.svg("SCARLET CINEMA | " + booking.code() + " | " + booking.totalAmount() + " VND"));
        return "thanh-toan";
    }

    /** Bấm "Thanh toán": đơn chuyển sang đã thanh toán, khách nhận vé có mã QR. */
    @PostMapping("/thanh-toan/{code}")
    public String pay(@PathVariable("code") String code,
                      @RequestParam(name = "cach", defaultValue = "") String method,
                      HttpSession session, RedirectAttributes redirect) {
        SessionUser user = Sessions.currentUser(session);
        try {
            if (bookingService.pay(user.id(), code, method)) {
                redirect.addFlashAttribute("vuaDat", true);
                return "redirect:/ve/" + code;
            }
        } catch (BookingException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
            return "redirect:/thanh-toan/" + code;
        }
        // Không thanh toán được: để trang thanh toán tự xét (đơn đã trả rồi, đã hết hạn, hoặc không còn)
        return "redirect:/thanh-toan/" + code;
    }

    /** Bấm "Áp dụng" ở ô mã ưu đãi: kiểm tra mã rồi trừ tiền cho đơn đang chờ thanh toán. */
    @PostMapping("/thanh-toan/{code}/ma")
    public String applyPromotion(@PathVariable("code") String code,
                                 @RequestParam(name = "ma", defaultValue = "") String promoCode,
                                 HttpSession session, RedirectAttributes redirect) {
        SessionUser user = Sessions.currentUser(session);
        try {
            int discount = bookingService.applyPromotion(user.id(), code, promoCode);
            redirect.addFlashAttribute("thongBao", "Đã áp mã " + promoCode.trim().toUpperCase(Locale.ROOT) + ": đơn được giảm " + fmt.tien(discount) + ".");
        } catch (BookingException e) {
            redirect.addFlashAttribute("loi", e.getMessage());
            redirect.addFlashAttribute("maNhap", promoCode.trim());
        } catch (ConcurrencyFailureException e) {
            redirect.addFlashAttribute("loi", "Đang có nhiều người dùng mã này cùng lúc. Bạn hãy bấm lại lần nữa.");
            redirect.addFlashAttribute("maNhap", promoCode.trim());
        }
        return "redirect:/thanh-toan/" + code;
    }

    /** Bấm "Bỏ mã": đơn trở lại tiền vé gốc. */
    @PostMapping("/thanh-toan/{code}/bo-ma")
    public String removePromotion(@PathVariable("code") String code, HttpSession session, RedirectAttributes redirect) {
        SessionUser user = Sessions.currentUser(session);
        if (bookingService.removePromotion(user.id(), code)) {
            redirect.addFlashAttribute("thongBao", "Đã bỏ mã ưu đãi khỏi đơn.");
        }
        return "redirect:/thanh-toan/" + code;
    }

    /** Bấm "Hủy đơn": xóa đơn chưa thanh toán để nhả ghế. */
    @PostMapping("/thanh-toan/{code}/huy")
    public String cancel(@PathVariable("code") String code, HttpSession session, RedirectAttributes redirect) {
        SessionUser user = Sessions.currentUser(session);
        BookingView booking = bookingDao.findByCode(code).filter(b -> b.userId() == user.id()).orElse(null);
        if (booking == null || !bookingService.cancel(user.id(), code)) {
            return "redirect:/ve-cua-toi";
        }
        redirect.addFlashAttribute("thongBao", "Đã hủy đơn " + code + ". Các ghế " + booking.seats() + " đã được nhả.");
        return "redirect:/dat-ve/" + booking.showtimeId();
    }

    /** Vé điện tử có mã QR. Chỉ có khi đơn đã thanh toán. Chỉ chủ vé và quản trị viên xem được. */
    @GetMapping("/ve/{code}")
    public String ticket(@PathVariable("code") String code, HttpSession session, Model model) {
        SessionUser user = Sessions.currentUser(session);
        BookingView booking = bookingDao.findByCode(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean owner = booking.userId() == user.id();
        if (!owner && !user.admin()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!booking.paid()) {
            // Chưa thanh toán thì chưa có vé: chủ đơn được đưa sang trang thanh toán
            if (owner) {
                return "redirect:/thanh-toan/" + code;
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("ve", booking);
        model.addAttribute("qr", qrService.svg(booking.code()));
        return "ve";
    }

    /** Lịch sử vé của người đang đăng nhập. */
    @GetMapping("/ve-cua-toi")
    public String history(HttpSession session, Model model) {
        SessionUser user = Sessions.currentUser(session);
        List<BookingView> bookings = bookingDao.findByUser(user.id());
        model.addAttribute("dsVe", bookings);
        model.addAttribute("tongVe", bookings.stream().mapToInt(BookingView::ticketCount).sum());
        return "lich-su-ve";
    }
}
