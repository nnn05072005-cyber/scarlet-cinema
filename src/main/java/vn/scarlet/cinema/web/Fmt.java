package vn.scarlet.cinema.web;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Các hàm định dạng dùng trong trang HTML, gọi bằng ${@fmt.tien(...)}, ${@fmt.ngay(...)}...
 */
@Component("fmt")
public class Fmt {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM");
    private static final String[] WEEKDAYS = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ nhật"};

    /** Mã của lần chạy web này, gắn sau địa chỉ file CSS và ảnh nền để trình duyệt không dùng lại bản cũ đã lưu. */
    private final String version = Long.toString(System.currentTimeMillis(), 36);

    public String phienBan() {
        return version;
    }

    /** 225000 thành "225.000 đ". */
    public String tien(Number amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount.longValue()) + " đ";
    }

    /** Ngày dạng 05.10.2026. */
    public String ngay(TemporalAccessor value) {
        return DATE.format(value);
    }

    /** Ngày và tháng dạng 05.10. */
    public String ngayThang(TemporalAccessor value) {
        return DAY_MONTH.format(value);
    }

    /** Giờ dạng 19:30. */
    public String gio(TemporalAccessor value) {
        return TIME.format(value);
    }

    /** Ngày trong tháng, hai chữ số: 05. */
    public String so(TemporalAccessor value) {
        return DAY.format(value);
    }

    /** Tháng, hai chữ số: 10. */
    public String thang(TemporalAccessor value) {
        return MONTH.format(value);
    }

    /** Thứ trong tuần: "Thứ Hai" ... "Chủ nhật". */
    public String thu(TemporalAccessor value) {
        return WEEKDAYS[DayOfWeek.of(value.get(ChronoField.DAY_OF_WEEK)).getValue() - 1];
    }

    /** Thứ viết ngắn cho ô chọn ngày: Chủ nhật thành "CN". */
    public String thuNgan(TemporalAccessor value) {
        String name = thu(value);
        return name.equals("Chủ nhật") ? "CN" : name;
    }

    /** Số có hai chữ số: 3 thành "03". */
    public String haiSo(Number value) {
        return String.format("%02d", value.intValue());
    }
}
