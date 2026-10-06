package vn.scarlet.cinema.model;

import java.time.LocalDate;

/** Tình hình sử dụng một mã ưu đãi (trang quản trị): số lượt đã dùng và tổng tiền đã giảm, chỉ tính đơn đã thanh toán. */
public record PromotionStat(String code, String title, Integer usageLimit, LocalDate endDate, boolean active, int uses,
                            long totalDiscount) {

    /** Mã còn dùng được vào ngày today không (đang bật, chưa hết hạn, chưa hết lượt). */
    public boolean usable(LocalDate today) {
        return active && !today.isAfter(endDate) && (usageLimit == null || uses < usageLimit);
    }

    public String usageText() {
        return usageLimit == null ? uses + " lượt" : uses + " / " + usageLimit + " lượt";
    }
}
