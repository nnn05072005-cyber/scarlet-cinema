package vn.scarlet.cinema.model;

/** Các hàm dùng chung để hiển thị phim. */
public final class MovieText {

    private MovieText() {
    }

    /** Giải thích phân loại độ tuổi (P, K, T13, T16, T18). */
    public static String note(String ageRating) {
        if (ageRating == null) {
            return "";
        }
        return switch (ageRating) {
            case "P" -> "Phim được phổ biến đến người xem ở mọi độ tuổi.";
            case "K" -> "Người xem dưới 13 tuổi cần có cha mẹ hoặc người giám hộ đi cùng.";
            case "T13" -> "Phim dành cho người xem từ đủ 13 tuổi trở lên.";
            case "T16" -> "Phim dành cho người xem từ đủ 16 tuổi trở lên.";
            case "T18" -> "Phim dành cho người xem từ đủ 18 tuổi trở lên.";
            default -> "";
        };
    }

    /** Tên càng dài thì chữ tiêu đề càng nhỏ: "" (bình thường), "vua", "dai". */
    public static String sizeClass(String title) {
        int length = title == null ? 0 : title.length();
        if (length > 30) {
            return "dai";
        }
        if (length > 18) {
            return "vua";
        }
        return "";
    }
}
