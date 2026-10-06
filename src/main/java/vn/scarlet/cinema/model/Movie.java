package vn.scarlet.cinema.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Một dòng của bảng movies. Thời lượng và độ tuổi có thể chưa có (null) với phim sắp chiếu.
 * trailerUrl là đường dẫn video trailer trên YouTube; để trống (null) nếu phim chưa có trailer.
 */
public record Movie(int id, String title, String originalTitle, String genre, Integer durationMin, String ageRating,
                    LocalDate releaseDate, String director, String description, String posterUrl, String backdropUrl,
                    String trailerUrl, String status) {

    /** Mã video YouTube gồm 11 ký tự, nằm sau "watch?v=", "youtu.be/", "embed/" hoặc "shorts/". */
    private static final Pattern YOUTUBE = Pattern.compile(
            "(?:youtu\\.be/|youtube(?:-nocookie)?\\.com/(?:watch\\?(?:[^#\\s]*&)?v=|embed/|shorts/|live/))([A-Za-z0-9_-]{11})(?![A-Za-z0-9_-])");

    /** Lấy mã video từ một đường dẫn YouTube. Trả về null nếu đường dẫn trống hoặc không phải video YouTube. */
    public static String youtubeIdOf(String url) {
        if (url == null) {
            return null;
        }
        Matcher m = YOUTUBE.matcher(url.trim());
        return m.find() ? m.group(1) : null;
    }

    /** Mã video trailer để phát ngay trên web; null nếu phim chưa có trailer. */
    public String youtubeId() {
        return youtubeIdOf(trailerUrl);
    }

    /** Dòng thông tin ngắn dưới tên phim, ví dụ "K · 109 phút · Hoạt hình, phiêu lưu". */
    public String info() {
        List<String> parts = new ArrayList<>();
        if (ageRating != null) {
            parts.add(ageRating);
        }
        if (durationMin != null) {
            parts.add(durationMin + " phút");
        }
        if (genre != null && !genre.isBlank()) {
            parts.add(genre);
        }
        return String.join(" · ", parts);
    }

    public boolean nowShowing() {
        return "NOW_SHOWING".equals(status);
    }

    public boolean comingSoon() {
        return "COMING_SOON".equals(status);
    }

    public String statusLabel() {
        return switch (status) {
            case "NOW_SHOWING" -> "Đang chiếu";
            case "COMING_SOON" -> "Sắp chiếu";
            default -> "Ngừng chiếu";
        };
    }

    /** Màu của nhãn trạng thái (tên lớp CSS). */
    public String statusColor() {
        return switch (status) {
            case "NOW_SHOWING" -> "xanh";
            case "COMING_SOON" -> "vang";
            default -> "xam";
        };
    }

    /** Giải thích phân loại độ tuổi theo quy định của Việt Nam. */
    public String ratingNote() {
        return MovieText.note(ageRating);
    }

    /** Tên phim dài thì dùng cỡ chữ nhỏ hơn cho tiêu đề lớn (tên lớp CSS). */
    public String sizeClass() {
        return MovieText.sizeClass(title);
    }
}
