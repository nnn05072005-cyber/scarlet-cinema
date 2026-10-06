-- =====================================================================
--  SCARLET CINEMA - TẠO CƠ SỞ DỮ LIỆU (MySQL 8)
--  Chạy file này trước, sau đó chạy du-lieu-mau.sql để có dữ liệu thử.
--  LƯU Ý: file này xóa 7 bảng cũ (nếu có) rồi tạo lại từ đầu.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS scarlet_cinema
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE scarlet_cinema;

-- Xóa theo thứ tự ngược với quan hệ (bảng con trước, bảng cha sau)
DROP TABLE IF EXISTS tickets;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS promotions;
DROP TABLE IF EXISTS showtimes;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS movies;
DROP TABLE IF EXISTS users;

-- ---------------------------------------------------------------------
-- 1. users: tài khoản của khách và của quản trị viên
-- ---------------------------------------------------------------------
CREATE TABLE users (
  id            INT          NOT NULL AUTO_INCREMENT,
  full_name     VARCHAR(100) NOT NULL                 COMMENT 'Họ và tên',
  email         VARCHAR(150) NOT NULL                 COMMENT 'Dùng để đăng nhập, không được trùng',
  password_hash VARCHAR(100) NOT NULL                 COMMENT 'Mật khẩu đã băm bằng BCrypt, không lưu mật khẩu gốc',
  role          ENUM('CUSTOMER', 'ADMIN') NOT NULL DEFAULT 'CUSTOMER' COMMENT 'CUSTOMER: khách, ADMIN: quản trị viên',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB COMMENT = 'Tài khoản';

-- ---------------------------------------------------------------------
-- 2. movies: phim
-- ---------------------------------------------------------------------
CREATE TABLE movies (
  id             INT          NOT NULL AUTO_INCREMENT,
  title          VARCHAR(200) NOT NULL                COMMENT 'Tên phim tiếng Việt',
  original_title VARCHAR(200) NULL                    COMMENT 'Tên gốc',
  genre          VARCHAR(100) NOT NULL                COMMENT 'Thể loại',
  duration_min   SMALLINT UNSIGNED NULL               COMMENT 'Thời lượng (phút); để trống khi chưa công bố',
  age_rating     ENUM('P', 'K', 'T13', 'T16', 'T18') NULL COMMENT 'Phân loại độ tuổi; để trống khi chưa công bố',
  release_date   DATE         NOT NULL                COMMENT 'Ngày khởi chiếu',
  director       VARCHAR(150) NULL                    COMMENT 'Đạo diễn',
  description    TEXT         NULL                    COMMENT 'Tóm tắt nội dung',
  poster_url     VARCHAR(500) NULL                    COMMENT 'Đường dẫn ảnh poster (tỉ lệ 2:3)',
  backdrop_url   VARCHAR(500) NULL                    COMMENT 'Đường dẫn ảnh ngang cho khối phim nổi bật',
  trailer_url    VARCHAR(500) NULL                    COMMENT 'Đường dẫn trailer trên YouTube; để trống nếu chưa có',
  status         ENUM('NOW_SHOWING', 'COMING_SOON', 'ENDED') NOT NULL DEFAULT 'COMING_SOON'
                                                      COMMENT 'Đang chiếu, sắp chiếu, đã ngừng chiếu',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_movies_status (status, release_date)
) ENGINE = InnoDB COMMENT = 'Phim';

-- ---------------------------------------------------------------------
-- 3. rooms: phòng chiếu. Ghế được đặt tên theo hàng (A, B, C...) và số (1, 2, 3...),
--    nên chỉ cần lưu số hàng và số ghế mỗi hàng, không cần bảng ghế riêng.
-- ---------------------------------------------------------------------
CREATE TABLE rooms (
  id            INT         NOT NULL AUTO_INCREMENT,
  name          VARCHAR(50) NOT NULL                  COMMENT 'Tên phòng',
  seat_rows     TINYINT UNSIGNED NOT NULL DEFAULT 8   COMMENT 'Số hàng ghế (8 hàng: A đến H)',
  seats_per_row TINYINT UNSIGNED NOT NULL DEFAULT 10  COMMENT 'Số ghế mỗi hàng',
  PRIMARY KEY (id),
  UNIQUE KEY uq_rooms_name (name),
  CONSTRAINT ck_rooms_size CHECK (seat_rows BETWEEN 1 AND 26 AND seats_per_row BETWEEN 1 AND 30)
) ENGINE = InnoDB COMMENT = 'Phòng chiếu';

-- ---------------------------------------------------------------------
-- 4. showtimes: suất chiếu = một phim chiếu ở một phòng vào một giờ
-- ---------------------------------------------------------------------
CREATE TABLE showtimes (
  id         INT          NOT NULL AUTO_INCREMENT,
  movie_id   INT          NOT NULL,
  room_id    INT          NOT NULL,
  start_time DATETIME     NOT NULL                    COMMENT 'Ngày giờ bắt đầu chiếu',
  price      INT UNSIGNED NOT NULL                    COMMENT 'Giá một vé (đồng)',
  PRIMARY KEY (id),
  -- Một phòng không thể có hai suất bắt đầu cùng một lúc
  UNIQUE KEY uq_showtimes_room_time (room_id, start_time),
  KEY idx_showtimes_movie_time (movie_id, start_time),
  KEY idx_showtimes_time (start_time),
  -- Không cho xóa phim hoặc phòng khi còn suất chiếu
  CONSTRAINT fk_showtimes_movie FOREIGN KEY (movie_id) REFERENCES movies (id),
  CONSTRAINT fk_showtimes_room  FOREIGN KEY (room_id)  REFERENCES rooms (id)
) ENGINE = InnoDB COMMENT = 'Suất chiếu';

-- ---------------------------------------------------------------------
-- 5. promotions: mã ưu đãi. Khách nhập mã ở trang thanh toán để được giảm tiền.
--    Có hai kiểu giảm: theo phần trăm (có thể đặt mức giảm tối đa) hoặc một số tiền cố định.
--    Mỗi mã có khoảng ngày dùng được, số tiền đơn tối thiểu và tổng số lượt dùng (có thể không giới hạn).
--    Quy tắc "mỗi tài khoản dùng mỗi mã một lần" do ứng dụng kiểm tra khi áp mã.
-- ---------------------------------------------------------------------
CREATE TABLE promotions (
  id             INT          NOT NULL AUTO_INCREMENT,
  code           VARCHAR(20)  NOT NULL                COMMENT 'Mã khách nhập, viết hoa không dấu, ví dụ SCARLET10',
  title          VARCHAR(100) NOT NULL                COMMENT 'Tên ưu đãi',
  description    VARCHAR(255) NOT NULL                COMMENT 'Mô tả ngắn hiện trên phiếu ưu đãi ở trang chủ',
  discount_type  ENUM('PERCENT', 'AMOUNT') NOT NULL   COMMENT 'PERCENT: giảm theo phần trăm; AMOUNT: giảm một số tiền cố định',
  discount_value INT UNSIGNED NOT NULL                COMMENT 'Số phần trăm (1 đến 100) hoặc số đồng được giảm',
  max_discount   INT UNSIGNED NULL                    COMMENT 'Mức giảm tối đa của mã PERCENT (đồng); để trống là không giới hạn',
  min_total      INT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Đơn phải từ số tiền này trở lên mới dùng được mã (đồng)',
  start_date     DATE         NOT NULL                COMMENT 'Ngày đầu dùng được',
  end_date       DATE         NOT NULL                COMMENT 'Ngày cuối dùng được',
  usage_limit    INT UNSIGNED NULL                    COMMENT 'Tổng số lượt dùng tối đa; để trống là không giới hạn',
  active         TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '1: đang bật; 0: đã tắt',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_promotions_code (code),
  -- Giá trị giảm phải lớn hơn 0; mã phần trăm thì không quá 100
  CONSTRAINT ck_promotions_value CHECK (discount_value > 0 AND (discount_type = 'AMOUNT' OR discount_value <= 100)),
  CONSTRAINT ck_promotions_dates CHECK (end_date >= start_date)
) ENGINE = InnoDB COMMENT = 'Mã ưu đãi';

-- ---------------------------------------------------------------------
-- 6. bookings: đơn đặt vé = một lần khách đặt một hoặc nhiều ghế của một suất chiếu
--    Đơn mới tạo ở trạng thái PENDING: ghế được giữ 10 phút để khách thanh toán.
--    Thanh toán xong thì đơn thành PAID và khách mới nhận được vé có mã QR.
--    Quá hạn mà chưa thanh toán, hoặc khách hủy, thì đơn bị xóa và ghế được nhả.
-- ---------------------------------------------------------------------
CREATE TABLE bookings (
  id             INT          NOT NULL AUTO_INCREMENT,
  booking_code   VARCHAR(20)  NOT NULL                COMMENT 'Mã đặt vé in trên vé và trong mã QR, ví dụ SC-20261005-0147',
  user_id        INT          NOT NULL                COMMENT 'Khách đặt',
  showtime_id    INT          NOT NULL                COMMENT 'Suất chiếu được đặt',
  total_amount   INT UNSIGNED NOT NULL                COMMENT 'Số tiền phải trả (đồng), đã trừ phần giảm giá nếu có',
  promotion_id   INT          NULL                    COMMENT 'Mã ưu đãi đã áp cho đơn; để trống là không dùng mã',
  discount_amount INT UNSIGNED NOT NULL DEFAULT 0     COMMENT 'Số tiền được giảm nhờ mã (đồng). Tiền vé gốc = total_amount + discount_amount',
  status         ENUM('PENDING', 'PAID') NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING: đang giữ ghế, chờ thanh toán; PAID: đã thanh toán',
  payment_method ENUM('BANK_QR', 'CARD', 'EWALLET') NULL COMMENT 'Cách thanh toán: chuyển khoản QR, thẻ, ví điện tử; để trống khi chưa thanh toán',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm đặt',
  expires_at     DATETIME     NULL                    COMMENT 'Hạn giữ ghế của đơn chưa thanh toán',
  paid_at        DATETIME     NULL                    COMMENT 'Thời điểm thanh toán',
  PRIMARY KEY (id),
  UNIQUE KEY uq_bookings_code (booking_code),
  -- Cặp (id, showtime_id) để bảng tickets tham chiếu, xem giải thích ở bảng tickets
  UNIQUE KEY uq_bookings_id_showtime (id, showtime_id),
  KEY idx_bookings_user (user_id, created_at),
  KEY idx_bookings_status (status, expires_at),
  KEY idx_bookings_promotion (promotion_id, user_id),
  -- Không cho xóa khách hoặc suất chiếu khi đã có đơn đặt vé
  CONSTRAINT fk_bookings_user     FOREIGN KEY (user_id)     REFERENCES users (id),
  CONSTRAINT fk_bookings_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes (id),
  -- Không cho xóa mã ưu đãi khi đã có đơn dùng mã đó (muốn ngừng thì tắt mã bằng cột active)
  CONSTRAINT fk_bookings_promotion FOREIGN KEY (promotion_id) REFERENCES promotions (id),
  -- Đơn chờ thanh toán phải có hạn giữ ghế; đơn đã thanh toán phải có cách thanh toán và thời điểm thanh toán
  CONSTRAINT ck_bookings_payment CHECK (
    (status = 'PENDING' AND expires_at IS NOT NULL AND paid_at IS NULL)
    OR (status = 'PAID' AND payment_method IS NOT NULL AND paid_at IS NOT NULL)),
  -- Có mã ưu đãi thì phải có số tiền giảm, không có mã thì số tiền giảm phải bằng 0
  CONSTRAINT ck_bookings_discount CHECK (
    (promotion_id IS NULL AND discount_amount = 0)
    OR (promotion_id IS NOT NULL AND discount_amount > 0))
) ENGINE = InnoDB COMMENT = 'Đơn đặt vé';

-- ---------------------------------------------------------------------
-- 7. tickets: vé = một ghế của một đơn đặt vé. Ghế của đơn đang chờ thanh toán cũng nằm ở đây (đang được giữ).
-- ---------------------------------------------------------------------
CREATE TABLE tickets (
  id          INT          NOT NULL AUTO_INCREMENT,
  booking_id  INT          NOT NULL,
  showtime_id INT          NOT NULL                   COMMENT 'Chép từ đơn đặt vé sang, để chặn bán trùng ghế',
  seat_code   VARCHAR(4)   NOT NULL                   COMMENT 'Mã ghế: chữ cái hàng + số ghế, ví dụ E4',
  price       INT UNSIGNED NOT NULL                   COMMENT 'Giá vé lúc đặt (đồng)',
  PRIMARY KEY (id),
  -- RÀNG BUỘC QUAN TRỌNG NHẤT: trong một suất chiếu, mỗi ghế chỉ bán được một lần.
  -- Hai người bấm đặt cùng một ghế cùng lúc thì MySQL chỉ nhận người đầu, người sau bị báo lỗi trùng.
  UNIQUE KEY uq_tickets_showtime_seat (showtime_id, seat_code),
  -- Vé phải thuộc đúng suất chiếu của đơn đặt vé. Xóa đơn thì xóa luôn các vé của đơn.
  CONSTRAINT fk_tickets_booking FOREIGN KEY (booking_id, showtime_id)
    REFERENCES bookings (id, showtime_id) ON DELETE CASCADE,
  CONSTRAINT ck_tickets_seat_code CHECK (seat_code REGEXP '^[A-Z][0-9]{1,2}$')
) ENGINE = InnoDB COMMENT = 'Vé';
