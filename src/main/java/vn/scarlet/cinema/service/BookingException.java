package vn.scarlet.cinema.service;

/** Lỗi khi đặt vé mà người dùng cần được báo (chọn sai ghế, suất đã chiếu...). */
public class BookingException extends RuntimeException {

    public BookingException(String message) {
        super(message);
    }
}
