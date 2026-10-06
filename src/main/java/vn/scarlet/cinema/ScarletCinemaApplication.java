package vn.scarlet.cinema;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Điểm bắt đầu của ứng dụng Scarlet Cinema.
 * Chạy hàm main là Spring Boot bật máy chủ web ở cổng 8080.
 */
@SpringBootApplication
@EnableScheduling   // cho phép chạy việc định kỳ: mỗi phút nhả ghế của đơn quá hạn chưa thanh toán
public class ScarletCinemaApplication {

    public static void main(String[] args) {
        // Rạp ở Việt Nam nên mọi phép tính ngày giờ dùng giờ Việt Nam, dù máy chủ đặt ở đâu
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(ScarletCinemaApplication.class, args);
    }
}
