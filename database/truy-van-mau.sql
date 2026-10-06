-- =====================================================================
--  SCARLET CINEMA - CÁC CÂU TRUY VẤN MẪU
--  Đây là những câu SQL mà trang web sẽ dùng. File này chỉ đọc dữ liệu, chạy bao nhiêu lần cũng được.
--  Trong mã Java, những chỗ ghi số cụ thể (mã phim, mã suất chiếu...) sẽ là dấu ? của PreparedStatement.
-- =====================================================================
USE scarlet_cinema;

-- 1. TRANG CHỦ: phim đang chiếu, phim mới ra xếp trước
SELECT id, title, genre, duration_min, age_rating, poster_url
FROM movies
WHERE status = 'NOW_SHOWING'
ORDER BY release_date DESC, id;

-- 2. TRANG CHỦ: phim sắp chiếu, xếp theo ngày khởi chiếu
SELECT id, title, genre, duration_min, age_rating, release_date, poster_url
FROM movies
WHERE status = 'COMING_SOON'
ORDER BY release_date, id;

-- 3. TRANG CHỦ: suất chiếu hôm nay kèm số ghế còn trống
--    Số ghế còn = tổng số ghế của phòng - số vé đã bán của suất đó
SELECT s.id,
       TIME_FORMAT(s.start_time, '%H:%i')            AS gio_chieu,
       m.title                                       AS phim,
       r.name                                        AS phong,
       m.age_rating                                  AS do_tuoi,
       r.seat_rows * r.seats_per_row - COUNT(t.id)   AS ghe_con
FROM showtimes s
JOIN movies m ON m.id = s.movie_id
JOIN rooms  r ON r.id = s.room_id
LEFT JOIN tickets t ON t.showtime_id = s.id
WHERE s.start_time >= CURDATE() AND s.start_time < CURDATE() + INTERVAL 1 DAY
GROUP BY s.id, s.start_time, m.title, r.name, m.age_rating, r.seat_rows, r.seats_per_row
ORDER BY s.start_time, r.id;

-- 4. CHI TIẾT PHIM: thông tin một phim (ví dụ phim có mã 1)
SELECT id, title, original_title, genre, duration_min, age_rating, release_date, director, description, poster_url, status
FROM movies
WHERE id = 1;

-- 5. CHI TIẾT PHIM: lịch chiếu của phim mã 1 trong ngày hôm nay
SELECT s.id, TIME_FORMAT(s.start_time, '%H:%i') AS gio_chieu, r.name AS phong, s.price AS gia_ve
FROM showtimes s
JOIN rooms r ON r.id = s.room_id
WHERE s.movie_id = 1
  AND s.start_time >= CURDATE() AND s.start_time < CURDATE() + INTERVAL 1 DAY
ORDER BY s.start_time;

-- 6. CHỌN GHẾ: thông tin suất chiếu mã 114 và các ghế đã bán (để tô màu "đã bán" trên sơ đồ ghế)
SELECT s.id, m.title AS phim, r.name AS phong, r.seat_rows, r.seats_per_row, s.start_time, s.price
FROM showtimes s
JOIN movies m ON m.id = s.movie_id
JOIN rooms  r ON r.id = s.room_id
WHERE s.id = 114;

SELECT seat_code
FROM tickets
WHERE showtime_id = 114
ORDER BY LEFT(seat_code, 1), CAST(SUBSTRING(seat_code, 2) AS UNSIGNED);

-- 7. ĐẶT VÉ VÀ THANH TOÁN (để trong chú thích vì đây là các lệnh ghi dữ liệu)
--
--    7a. Giữ ghế: toàn bộ các lệnh nằm trong MỘT giao dịch (transaction). Đơn mới ở trạng thái PENDING,
--    ghế được giữ 10 phút. Nếu có ghế đã bị người khác đặt hoặc đang giữ, lệnh INSERT vào tickets
--    báo lỗi trùng (mã lỗi 1062) và ứng dụng ROLLBACK, không có gì được lưu.
--
--    START TRANSACTION;
--    INSERT INTO bookings (booking_code, user_id, showtime_id, total_amount, status, created_at, expires_at)
--    VALUES (CONCAT('T', LEFT(REPLACE(UUID(), '-', ''), 19)), 3, 120, 150000,
--            'PENDING', NOW(), NOW() + INTERVAL 10 MINUTE);                       -- mã tạm, đổi lại ngay bên dưới
--    SET @don = LAST_INSERT_ID();
--    INSERT INTO tickets (booking_id, showtime_id, seat_code, price)
--    VALUES (@don, 120, 'A1', 75000), (@don, 120, 'A2', 75000);
--    UPDATE bookings b JOIN showtimes s ON s.id = b.showtime_id
--    SET b.booking_code = CONCAT('SC-', DATE_FORMAT(s.start_time, '%Y%m%d'), '-', LPAD(b.id, 4, '0'))
--    WHERE b.id = @don;
--    COMMIT;
--
--    7b. Thanh toán: một lệnh UPDATE, chỉ có tác dụng khi đơn là của đúng khách, còn chờ thanh toán và còn hạn.
--    Lệnh sửa được 1 dòng là thanh toán thành công; 0 dòng là đơn đã hết hạn hoặc không tồn tại.
--
--    UPDATE bookings SET status = 'PAID', payment_method = 'BANK_QR', paid_at = NOW()
--    WHERE booking_code = 'SC-20261005-0148' AND user_id = 3 AND status = 'PENDING' AND expires_at > NOW();
--
--    7c. Áp mã ưu đãi cho đơn đang chờ thanh toán (mã đã qua các bước kiểm tra ở câu 16). Trong lệnh UPDATE một bảng,
--    MySQL gán từ trái sang phải: ghi số tiền giảm trước, rồi mới trừ vào số tiền phải trả.
--
--    UPDATE bookings SET promotion_id = 1, discount_amount = 15000, total_amount = total_amount - discount_amount
--    WHERE booking_code = 'SC-20261005-0148' AND user_id = 3 AND status = 'PENDING' AND expires_at > NOW();
--
--    Bỏ mã: trả lại số tiền gốc trước, rồi mới xóa số tiền giảm.
--
--    UPDATE bookings SET total_amount = total_amount + discount_amount, discount_amount = 0, promotion_id = NULL
--    WHERE booking_code = 'SC-20261005-0148' AND user_id = 3 AND status = 'PENDING';
--
--    7d. Nhả ghế: xóa các đơn quá hạn chưa thanh toán (web tự chạy mỗi phút). Xóa đơn thì các vé của đơn
--    bị xóa theo (ON DELETE CASCADE), nên ghế bán lại được.
--
--    DELETE FROM bookings WHERE status = 'PENDING' AND expires_at <= NOW();

-- 8. VÉ: thông tin in trên vé và trong mã QR, tìm theo mã đặt vé (lấy đơn mới nhất của khách mã 2 làm ví dụ).
--    Web chỉ hiện vé có mã QR khi status = 'PAID'; đơn còn PENDING thì chuyển sang trang thanh toán.
--    LEFT JOIN promotions vì phần lớn đơn không dùng mã ưu đãi.
SELECT b.booking_code, b.status, b.payment_method, b.paid_at,
       m.title AS phim, m.age_rating, m.poster_url, s.start_time, r.name AS phong,
       GROUP_CONCAT(t.seat_code ORDER BY t.seat_code SEPARATOR ', ') AS ghe,
       COUNT(t.id) AS so_ve, b.total_amount AS phai_tra, b.discount_amount AS da_giam, p.code AS ma_uu_dai
FROM bookings b
JOIN showtimes s ON s.id = b.showtime_id
JOIN movies    m ON m.id = s.movie_id
JOIN rooms     r ON r.id = s.room_id
JOIN tickets   t ON t.booking_id = b.id
LEFT JOIN promotions p ON p.id = b.promotion_id
WHERE b.id = (SELECT MAX(id) FROM bookings WHERE user_id = 2)
GROUP BY b.id, b.booking_code, b.status, b.payment_method, b.paid_at,
         m.title, m.age_rating, m.poster_url, s.start_time, r.name, b.total_amount, b.discount_amount, p.code;

-- 9. LỊCH SỬ VÉ của khách mã 2, đơn mới nhất ở trên. Đơn chưa trả tiền ghi "Chờ thanh toán"; suất chưa chiếu ghi "Sắp chiếu".
SELECT b.booking_code, m.title AS phim, s.start_time, r.name AS phong,
       GROUP_CONCAT(t.seat_code ORDER BY t.seat_code SEPARATOR ', ') AS ghe,
       b.total_amount AS tong_tien,
       CASE WHEN b.status = 'PENDING' THEN 'Chờ thanh toán'
            WHEN s.start_time > NOW() THEN 'Sắp chiếu'
            ELSE 'Đã chiếu' END AS trang_thai
FROM bookings b
JOIN showtimes s ON s.id = b.showtime_id
JOIN movies    m ON m.id = s.movie_id
JOIN rooms     r ON r.id = s.room_id
JOIN tickets   t ON t.booking_id = b.id
WHERE b.user_id = 2
GROUP BY b.id, b.booking_code, b.status, m.title, s.start_time, r.name, b.total_amount
ORDER BY b.created_at DESC;

-- 10. ĐĂNG NHẬP: lấy tài khoản theo email, sau đó mã Java so mật khẩu người dùng nhập với password_hash
SELECT id, full_name, email, password_hash, role
FROM users
WHERE email = 'minhanh@example.com';

-- 11. QUẢN TRỊ: danh sách suất chiếu sắp tới kèm số ghế đã bán
SELECT s.id, m.title AS phim, r.name AS phong, s.start_time, s.price AS gia_ve,
       COUNT(t.id) AS da_ban, r.seat_rows * r.seats_per_row AS tong_ghe
FROM showtimes s
JOIN movies m ON m.id = s.movie_id
JOIN rooms  r ON r.id = s.room_id
LEFT JOIN tickets t ON t.showtime_id = s.id
WHERE s.start_time >= CURDATE()
GROUP BY s.id, m.title, r.name, s.start_time, s.price, r.seat_rows, r.seats_per_row
ORDER BY s.start_time, r.id
LIMIT 15;

-- 12. QUẢN TRỊ: vé đã đặt, mới nhất ở trên, có thể tìm theo mã vé hoặc tên khách
SELECT b.booking_code, u.full_name AS khach, m.title AS phim, s.start_time,
       GROUP_CONCAT(t.seat_code ORDER BY t.seat_code SEPARATOR ', ') AS ghe,
       b.total_amount AS phai_tra, b.discount_amount AS da_giam, b.status AS trang_thai, b.created_at AS dat_luc
FROM bookings b
JOIN users     u ON u.id = b.user_id
JOIN showtimes s ON s.id = b.showtime_id
JOIN movies    m ON m.id = s.movie_id
JOIN tickets   t ON t.booking_id = b.id
WHERE b.booking_code LIKE CONCAT('%', '', '%') OR u.full_name LIKE CONCAT('%', '', '%')
GROUP BY b.id, b.booking_code, u.full_name, m.title, s.start_time, b.total_amount, b.discount_amount, b.status, b.created_at
ORDER BY b.created_at DESC
LIMIT 10;

-- 13. QUẢN TRỊ: trước khi thêm suất chiếu, kiểm tra phòng có bị trùng giờ với suất khác không.
--     Ví dụ: định thêm suất ở phòng 1, bắt đầu 20:00 hôm nay, phim dài 109 phút.
--     Hai suất trùng nhau khi suất này bắt đầu trước lúc suất kia kết thúc và ngược lại.
SELECT s.id, m.title AS phim_bi_trung, s.start_time,
       s.start_time + INTERVAL m.duration_min MINUTE AS ket_thuc
FROM showtimes s
JOIN movies m ON m.id = s.movie_id
WHERE s.room_id = 1
  AND s.start_time < TIMESTAMP(CURDATE(), '20:00:00') + INTERVAL 109 MINUTE
  AND s.start_time + INTERVAL m.duration_min MINUTE > TIMESTAMP(CURDATE(), '20:00:00');

-- 14. THỐNG KÊ: số vé và doanh thu theo từng phim, chỉ tính đơn đã thanh toán.
--     Doanh thu là số tiền khách thật sự trả (đã trừ giảm giá), nên cộng theo ĐƠN chứ không cộng giá từng vé.
--     Bảng con x đếm số vé của mỗi đơn, để một đơn nhiều vé không bị cộng tiền nhiều lần.
SELECT m.title AS phim, SUM(x.so_ve) AS so_ve, SUM(b.total_amount) AS doanh_thu, SUM(b.discount_amount) AS da_giam
FROM bookings b
JOIN (SELECT booking_id, COUNT(*) AS so_ve FROM tickets GROUP BY booking_id) x ON x.booking_id = b.id
JOIN showtimes s ON s.id = b.showtime_id
JOIN movies    m ON m.id = s.movie_id
WHERE b.status = 'PAID'
GROUP BY m.id, m.title
ORDER BY doanh_thu DESC;

-- 15. TRANG CHỦ: các mã ưu đãi đang dùng được (đang bật, còn trong hạn, chưa hết lượt), tối đa 4 mã.
--     Lượt đã dùng gồm đơn đã thanh toán và đơn đang giữ ghế còn hạn.
SELECT p.code, p.title, p.description, p.discount_type, p.discount_value, p.end_date
FROM promotions p
WHERE p.active = 1 AND CURDATE() BETWEEN p.start_date AND p.end_date
  AND (p.usage_limit IS NULL OR p.usage_limit >
       (SELECT COUNT(*) FROM bookings b
        WHERE b.promotion_id = p.id AND (b.status = 'PAID' OR b.expires_at > NOW())))
ORDER BY p.id
LIMIT 4;

-- 16. THANH TOÁN: kiểm tra một mã khách nhập (ví dụ khách mã 3 nhập SCARLET10).
--     Câu a lấy mã; trong ứng dụng câu này có thêm FOR UPDATE để hai người áp cùng một mã phải chờ nhau.
SELECT id, code, discount_type, discount_value, max_discount, min_total, start_date, end_date, usage_limit, active
FROM promotions
WHERE code = 'SCARLET10';

--     Câu b đếm tổng số lượt mã đã được dùng và số lượt của riêng khách này (mỗi tài khoản dùng mỗi mã một lần).
SELECT COUNT(*) AS tong_luot, COALESCE(SUM(b.user_id = 3), 0) AS luot_cua_khach
FROM bookings b
WHERE b.promotion_id = (SELECT id FROM promotions WHERE code = 'SCARLET10')
  AND (b.status = 'PAID' OR b.expires_at > NOW());

-- 17. QUẢN TRỊ: mỗi mã ưu đãi đã được dùng bao nhiêu lượt và giảm tổng cộng bao nhiêu tiền (chỉ tính đơn đã thanh toán)
SELECT p.code, p.title, p.usage_limit, p.end_date, COUNT(b.id) AS luot_dung, COALESCE(SUM(b.discount_amount), 0) AS tong_giam
FROM promotions p
LEFT JOIN bookings b ON b.promotion_id = p.id AND b.status = 'PAID'
GROUP BY p.id, p.code, p.title, p.usage_limit, p.end_date
ORDER BY p.id;
