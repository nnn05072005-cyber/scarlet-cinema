-- =====================================================================
--  SCARLET CINEMA - KIỂM TRA CÁC RÀNG BUỘC
--  Mỗi lệnh đánh số dưới đây (13 lệnh) CỐ TÌNH làm sai, nên MySQL PHẢI báo lỗi. Báo lỗi nghĩa là ràng buộc hoạt động.
--  Cách chạy bằng dòng lệnh:  mysql -u root -p --force --verbose < kiem-tra-rang-buoc.sql
--  (Trong MySQL Workbench: bôi đen từng lệnh rồi bấm Ctrl + Enter để chạy riêng lệnh đó.)
--  Không lệnh nào làm thay đổi dữ liệu mẫu.
-- =====================================================================
USE scarlet_cinema;

-- 1. Bán trùng ghế: ghế E4 của suất 114 đã có người đặt -> lỗi 1062 (Duplicate entry)
INSERT INTO tickets (booking_id, showtime_id, seat_code, price) VALUES (146, 114, 'E4', 75000);

-- 2. Vé ghi sai suất chiếu so với đơn đặt vé: đơn 146 thuộc suất 114, không phải suất 115 -> lỗi 1452
INSERT INTO tickets (booking_id, showtime_id, seat_code, price) VALUES (146, 115, 'A1', 75000);

-- 3. Mã ghế sai dạng -> lỗi 3819 (Check constraint)
INSERT INTO tickets (booking_id, showtime_id, seat_code, price) VALUES (146, 114, 'ghe1', 75000);

-- 4. Đăng ký trùng email -> lỗi 1062
INSERT INTO users (full_name, email, password_hash) VALUES ('Người Thử', 'minhanh@example.com', 'x');

-- 5. Hai suất chiếu cùng phòng cùng giờ bắt đầu -> lỗi 1062
INSERT INTO showtimes (movie_id, room_id, start_time, price)
SELECT movie_id, room_id, start_time, price FROM showtimes WHERE id = 114;

-- 6. Xóa phim đang có suất chiếu -> lỗi 1451 (không cho xóa bảng cha khi bảng con còn tham chiếu)
DELETE FROM movies WHERE id = 1;

-- 7. Xóa suất chiếu đã có người đặt vé -> lỗi 1451
DELETE FROM showtimes WHERE id = 114;

-- 8. Suất chiếu cho phim không tồn tại -> lỗi 1452
INSERT INTO showtimes (movie_id, room_id, start_time, price) VALUES (999, 1, '2030-01-01 10:00:00', 60000);

-- 9. Đơn chờ thanh toán mà không có hạn giữ ghế -> lỗi 3819 (Check constraint ck_bookings_payment)
INSERT INTO bookings (booking_code, user_id, showtime_id, total_amount, status) VALUES ('THU-RANG-BUOC-9', 2, 114, 75000, 'PENDING');

-- 10. Đánh dấu đã thanh toán mà không ghi cách thanh toán và thời điểm thanh toán -> lỗi 3819
INSERT INTO bookings (booking_code, user_id, showtime_id, total_amount, status) VALUES ('THU-RANG-BUOC-10', 2, 114, 75000, 'PAID');

-- Sau các lệnh trên, số dòng phải giữ nguyên như lúc mới nạp dữ liệu mẫu
SELECT (SELECT COUNT(*) FROM users) AS users, (SELECT COUNT(*) FROM movies) AS movies,
       (SELECT COUNT(*) FROM showtimes) AS showtimes, (SELECT COUNT(*) FROM bookings) AS bookings,
       (SELECT COUNT(*) FROM tickets) AS tickets;

-- 11. Mã ưu đãi giảm 150 phần trăm -> lỗi 3819 (Check constraint ck_promotions_value)
INSERT INTO promotions (code, title, description, discount_type, discount_value, start_date, end_date)
VALUES ('THU11', 'Thử', 'Thử ràng buộc', 'PERCENT', 150, CURDATE(), CURDATE());

-- 12. Trùng mã ưu đãi đã có -> lỗi 1062
INSERT INTO promotions (code, title, description, discount_type, discount_value, start_date, end_date)
VALUES ('SCARLET10', 'Thử', 'Thử ràng buộc', 'PERCENT', 5, CURDATE(), CURDATE());

-- 13. Ghi số tiền giảm cho một đơn không có mã ưu đãi -> lỗi 3819 (Check constraint ck_bookings_discount)
UPDATE bookings SET discount_amount = 10000 WHERE id = 147;
