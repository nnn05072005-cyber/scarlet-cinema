package vn.scarlet.cinema.model;

import java.time.LocalDateTime;

/**
 * Một đơn đặt vé kèm tên khách, phim, suất chiếu và danh sách ghế.
 * Lấy từ bảng bookings nối với users, showtimes, movies, rooms, tickets.
 *
 * Trạng thái (status): PENDING là đang giữ ghế chờ thanh toán, PAID là đã thanh toán.
 * secondsLeft là số giây còn lại của hạn giữ ghế (chỉ có nghĩa với đơn PENDING).
 *
 * totalAmount là số tiền phải trả, đã trừ discountAmount (số tiền giảm nhờ mã ưu đãi promoCode, nếu có).
 */
public record BookingView(int id, String code, int userId, String customerName, int showtimeId, int movieId,
                          String movieTitle, String ageRating, String posterUrl, LocalDateTime startTime, String roomName,
                          String seats, int ticketCount, int totalAmount, LocalDateTime createdAt, String status,
                          String paymentMethod, LocalDateTime paidAt, Integer secondsLeft, int discountAmount, String promoCode) {

    /** Đã thanh toán: khách được nhận vé có mã QR. */
    public boolean paid() {
        return "PAID".equals(status);
    }

    /** Đang chờ thanh toán. */
    public boolean pending() {
        return "PENDING".equals(status);
    }

    /** Đơn chờ thanh toán đã quá hạn giữ ghế. */
    public boolean expired() {
        return pending() && (secondsLeft == null || secondsLeft <= 0);
    }

    /** Tên cách thanh toán để hiển thị. */
    public String methodLabel() {
        if (paymentMethod == null) {
            return "";
        }
        return switch (paymentMethod) {
            case "BANK_QR" -> "chuyển khoản QR";
            case "CARD" -> "thẻ ngân hàng";
            case "EWALLET" -> "ví điện tử";
            default -> paymentMethod;
        };
    }

    /** Tiền vé gốc, trước khi trừ giảm giá. */
    public int subtotal() {
        return totalAmount + discountAmount;
    }

    /** Đơn có dùng mã ưu đãi. */
    public boolean discounted() {
        return promoCode != null && discountAmount > 0;
    }

    /** Giá một vé của đơn (giá gốc). */
    public int unitPrice() {
        return ticketCount == 0 ? 0 : subtotal() / ticketCount;
    }

    /** Suất chưa chiếu. */
    public boolean upcoming() {
        return startTime.isAfter(LocalDateTime.now());
    }

    /** Số phòng in trên vé: "Phòng 3" thành "03". */
    public String roomShort() {
        String number = roomName.replace("Phòng", "").trim();
        return number.matches("\\d") ? "0" + number : number;
    }

    public String ratingNote() {
        return MovieText.note(ageRating);
    }

    public String sizeClass() {
        return MovieText.sizeClass(movieTitle);
    }
}
