package vn.scarlet.cinema.model;

/** Một dòng của bảng rooms. Ghế được gọi theo hàng (A, B, C...) và số (1, 2, 3...). */
public record Room(int id, String name, int seatRows, int seatsPerRow) {

    public int totalSeats() {
        return seatRows * seatsPerRow;
    }
}
