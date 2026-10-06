package vn.scarlet.cinema.model;

import java.time.LocalDate;

/**
 * Một dòng của bảng promotions (mã ưu đãi).
 * discountType: PERCENT là giảm theo phần trăm (có thể có mức tối đa maxDiscount), AMOUNT là giảm một số tiền cố định.
 * maxDiscount và usageLimit để trống (null) nghĩa là không giới hạn.
 */
public record Promotion(int id, String code, String title, String description, String discountType, int discountValue,
                        Integer maxDiscount, int minTotal, LocalDate startDate, LocalDate endDate, Integer usageLimit,
                        boolean active) {

    public boolean percent() {
        return "PERCENT".equals(discountType);
    }

    /** Số tiền được giảm cho một đơn có tiền vé gốc là subtotal. Không bao giờ giảm quá tiền vé. */
    public int discountFor(int subtotal) {
        long discount;
        if (percent()) {
            discount = Math.round(subtotal * (discountValue / 100.0));
            if (maxDiscount != null) {
                discount = Math.min(discount, maxDiscount);
            }
        } else {
            discount = discountValue;
        }
        return (int) Math.max(0, Math.min(discount, subtotal));
    }

    /** Chữ lớn in trên phiếu ưu đãi: "10%" hoặc "20K". */
    public String label() {
        if (percent()) {
            return discountValue + "%";
        }
        return discountValue % 1000 == 0 ? (discountValue / 1000) + "K" : String.valueOf(discountValue);
    }

    /** Hôm nay (today) có nằm trong khoảng ngày dùng được của mã không. */
    public boolean validOn(LocalDate today) {
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }
}
