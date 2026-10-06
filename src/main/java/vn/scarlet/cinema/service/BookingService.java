package vn.scarlet.cinema.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.scarlet.cinema.dao.BookingDao;
import vn.scarlet.cinema.dao.PromotionDao;
import vn.scarlet.cinema.dao.ShowtimeDao;
import vn.scarlet.cinema.model.BookingView;
import vn.scarlet.cinema.model.Promotion;
import vn.scarlet.cinema.model.ShowtimeView;

/**
 * Nghiệp vụ đặt vé và thanh toán.
 *
 * Trình tự: khách chọn ghế, web giữ ghế (đơn PENDING) trong 10 phút, khách thanh toán thì đơn thành PAID
 * và lúc đó mới có vé kèm mã QR. Quá 10 phút chưa thanh toán thì đơn bị xóa, ghế bán lại được.
 * Trong lúc chờ thanh toán khách có thể nhập một mã ưu đãi để được giảm tiền.
 */
@Service
public class BookingService {

    /** Mỗi lần đặt tối đa 8 ghế. */
    public static final int MAX_SEATS = 8;

    /** Số phút giữ ghế trong lúc chờ thanh toán. */
    public static final int HOLD_MINUTES = 10;

    /** Các cách thanh toán web nhận: chuyển khoản QR, thẻ ngân hàng, ví điện tử. */
    public static final Set<String> PAYMENT_METHODS = Set.of("BANK_QR", "CARD", "EWALLET");

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingDao bookingDao;
    private final ShowtimeDao showtimeDao;
    private final PromotionDao promotionDao;

    public BookingService(BookingDao bookingDao, ShowtimeDao showtimeDao, PromotionDao promotionDao) {
        this.bookingDao = bookingDao;
        this.showtimeDao = showtimeDao;
        this.promotionDao = promotionDao;
    }

    /**
     * Giữ các ghế đã chọn của một suất chiếu để chờ thanh toán, trả về mã đặt vé.
     *
     * Toàn bộ các lệnh ghi dữ liệu nằm trong MỘT giao dịch (@Transactional):
     * thêm đơn, thêm từng vé, đổi mã đơn. Nếu có một ghế đã bị người khác đặt hoặc đang giữ,
     * lệnh thêm vé báo lỗi trùng, Spring ROLLBACK cả giao dịch nên không có gì được lưu.
     */
    @Transactional
    public String hold(int userId, int showtimeId, List<String> seatCodes) {
        ShowtimeView showtime = showtimeDao.findById(showtimeId)
                .orElseThrow(() -> new BookingException("Không tìm thấy suất chiếu."));
        if (showtime.started()) {
            throw new BookingException("Suất chiếu này đã bắt đầu nên không đặt vé được nữa.");
        }
        List<String> seats = checkSeats(showtime, seatCodes);

        // 1. Thêm đơn chờ thanh toán với mã tạm (mã chính thức cần số thứ tự của đơn, chỉ có sau khi thêm)
        int total = showtime.price() * seats.size();
        String temporaryCode = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 19);
        int bookingId = bookingDao.insertBooking(temporaryCode, userId, showtimeId, total, HOLD_MINUTES);

        // 2. Thêm từng vé (giữ ghế). Ghế đã có người đặt hoặc đang giữ thì lệnh này ném ngoại lệ và cả giao dịch bị hủy.
        for (String seat : seats) {
            bookingDao.insertTicket(bookingId, showtimeId, seat, showtime.price());
        }

        // 3. Đổi sang mã chính thức: SC-<ngày chiếu>-<số thứ tự đơn>
        String day = showtime.startTime().format(DateTimeFormatter.BASIC_ISO_DATE);
        String code = String.format("SC-%s-%04d", day, bookingId);
        bookingDao.updateCode(bookingId, code);
        return code;
    }

    /**
     * Thanh toán một đơn đang chờ. Trả về true nếu thanh toán được;
     * false nếu đơn không còn (đã hết hạn giữ ghế, đã bị hủy) hoặc không phải của khách này.
     * Đây là thanh toán thử: web không kết nối ngân hàng, chỉ ghi nhận khách đã bấm thanh toán.
     */
    public boolean pay(int userId, String bookingCode, String paymentMethod) {
        if (!PAYMENT_METHODS.contains(paymentMethod)) {
            throw new BookingException("Bạn chưa chọn cách thanh toán.");
        }
        return bookingDao.markPaid(bookingCode, userId, paymentMethod) == 1;
    }

    /**
     * Áp một mã ưu đãi cho đơn đang chờ thanh toán, trả về số tiền được giảm.
     * Mỗi đơn chỉ dùng một mã: áp mã mới thì mã cũ (nếu có) bị thay.
     *
     * Các bước kiểm tra, sai bước nào thì báo lý do bước đó:
     * mã có tồn tại, đang bật, còn trong hạn; đơn đủ số tiền tối thiểu; mã còn lượt; khách chưa dùng mã này.
     * Cả hàm chạy trong một giao dịch và dòng của mã bị khóa (FOR UPDATE) từ lúc đọc tới lúc ghi,
     * nên hai người cùng áp một mã chỉ còn một lượt thì chỉ một người được.
     */
    @Transactional
    public int applyPromotion(int userId, String bookingCode, String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            throw new BookingException("Bạn chưa nhập mã ưu đãi.");
        }
        if (!code.matches("[A-Z0-9]{3,20}")) {
            throw new BookingException("Mã ưu đãi chỉ gồm chữ cái và chữ số, dài từ 3 đến 20 ký tự.");
        }
        BookingView booking = bookingDao.findByCode(bookingCode)
                .filter(b -> b.userId() == userId && b.pending() && !b.expired())
                .orElseThrow(() -> new BookingException("Đơn này không còn chờ thanh toán nên không áp mã được."));

        Promotion promotion = promotionDao.findByCodeForUpdate(code)
                .orElseThrow(() -> new BookingException("Không có mã ưu đãi \"" + code + "\"."));
        LocalDate today = LocalDate.now();
        if (!promotion.active()) {
            throw new BookingException("Mã " + code + " đã ngừng áp dụng.");
        }
        if (today.isBefore(promotion.startDate())) {
            throw new BookingException("Mã " + code + " chưa tới ngày áp dụng.");
        }
        if (today.isAfter(promotion.endDate())) {
            throw new BookingException("Mã " + code + " đã hết hạn.");
        }
        int subtotal = booking.subtotal();
        if (subtotal < promotion.minTotal()) {
            throw new BookingException("Mã " + code + " chỉ áp dụng cho đơn từ " + money(promotion.minTotal()) + ". Đơn của bạn là " + money(subtotal) + ".");
        }
        if (promotion.usageLimit() != null && promotionDao.countUses(promotion.id(), booking.id()) >= promotion.usageLimit()) {
            throw new BookingException("Mã " + code + " đã hết lượt dùng.");
        }
        if (promotionDao.countUsesByUser(promotion.id(), userId, booking.id()) > 0) {
            throw new BookingException("Bạn đã dùng mã " + code + " rồi. Mỗi tài khoản dùng mỗi mã một lần.");
        }
        int discount = promotion.discountFor(subtotal);
        if (discount <= 0 || bookingDao.setPromotion(booking.id(), promotion.id(), discount, subtotal) != 1) {
            throw new BookingException("Chưa áp được mã " + code + ". Bạn hãy thử lại.");
        }
        return discount;
    }

    /** Bỏ mã ưu đãi khỏi đơn đang chờ thanh toán. Trả về true nếu đơn đang có mã và đã bỏ được. */
    public boolean removePromotion(int userId, String bookingCode) {
        return bookingDao.clearPromotion(bookingCode, userId) == 1;
    }

    /** Số tiền dạng "100.000 đ" dùng trong câu thông báo. */
    private static String money(int amount) {
        return String.format(Locale.ROOT, "%,d", amount).replace(',', '.') + " đ";
    }

    /** Khách hủy đơn chưa thanh toán: xóa đơn để nhả ghế. Trả về true nếu có đơn bị xóa. */
    public boolean cancel(int userId, String bookingCode) {
        return bookingDao.deletePending(bookingCode, userId) == 1;
    }

    /**
     * Nhả ghế của các đơn quá hạn chưa thanh toán. Spring tự gọi hàm này mỗi phút một lần;
     * trang chọn ghế cũng gọi để sơ đồ ghế luôn đúng.
     */
    @Scheduled(fixedDelay = 60000)
    public void releaseExpired() {
        try {
            int removed = bookingDao.deleteExpired();
            if (removed > 0) {
                log.info("Da nha ghe cua {} don qua han chua thanh toan.", removed);
            }
        } catch (RuntimeException e) {
            log.warn("Chua nha duoc ghe cua don qua han: {}", e.getMessage());
        }
    }

    /** Kiểm tra danh sách ghế gửi lên: không rỗng, không quá 8 ghế, mã ghế phải có thật trong phòng. */
    private List<String> checkSeats(ShowtimeView showtime, List<String> seatCodes) {
        if (seatCodes == null || seatCodes.isEmpty()) {
            throw new BookingException("Bạn chưa chọn ghế nào.");
        }
        Set<String> unique = new LinkedHashSet<>(seatCodes);
        if (unique.size() > MAX_SEATS) {
            throw new BookingException("Mỗi lần đặt tối đa " + MAX_SEATS + " ghế.");
        }
        for (String code : unique) {
            if (!validSeat(code, showtime.seatRows(), showtime.seatsPerRow())) {
                throw new BookingException("Mã ghế không hợp lệ: " + code);
            }
        }
        return new ArrayList<>(unique);
    }

    /** Mã ghế gồm chữ cái hàng (A, B, C...) và số ghế, ví dụ E4. */
    static boolean validSeat(String code, int seatRows, int seatsPerRow) {
        if (code == null || !code.matches("[A-Z][0-9]{1,2}")) {
            return false;
        }
        int row = code.charAt(0) - 'A';
        int number = Integer.parseInt(code.substring(1));
        return row < seatRows && number >= 1 && number <= seatsPerRow;
    }
}
