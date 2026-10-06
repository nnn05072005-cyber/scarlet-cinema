package vn.scarlet.cinema.model;

import java.time.LocalDateTime;

/**
 * Một suất chiếu kèm thông tin phim, phòng và số vé đã bán.
 * Lấy từ bảng showtimes nối với movies, rooms và đếm trong tickets.
 */
public record ShowtimeView(int id, int movieId, String movieTitle, String ageRating, Integer durationMin, String posterUrl,
                           int roomId, String roomName, int seatRows, int seatsPerRow, LocalDateTime startTime, int price,
                           int sold) {

    public int totalSeats() {
        return seatRows * seatsPerRow;
    }

    public int seatsLeft() {
        return totalSeats() - sold;
    }

    /** Sắp hết ghế: còn từ 10 ghế trở xuống. */
    public boolean almostFull() {
        return seatsLeft() <= 10;
    }

    /** Suất đã bắt đầu chiếu thì không đặt vé được nữa. */
    public boolean started() {
        return !startTime.isAfter(LocalDateTime.now());
    }

    public String sizeClass() {
        return MovieText.sizeClass(movieTitle);
    }
}
