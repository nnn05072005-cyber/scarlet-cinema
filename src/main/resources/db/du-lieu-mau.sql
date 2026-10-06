-- =====================================================================
--  SCARLET CINEMA - DỮ LIỆU MẪU (chạy sau schema.sql)
--  Chạy lại file này lúc nào cũng được: nó xóa dữ liệu cũ rồi nạp lại.
--
--  Lịch chiếu được tạo THEO NGÀY CHẠY FILE: 7 ngày tính từ hôm nay, mỗi ngày 15 suất.
--  Nhờ vậy lúc nào mở web cũng có suất chiếu của "hôm nay". Muốn làm mới lịch thì chạy lại file.
--
--  TÀI KHOẢN THỬ (chỉ dùng để chạy thử, đổi mật khẩu trước khi dùng thật)
--    Quản trị viên : admin@example.com    mật khẩu: quantri2026
--    Khách         : minhanh@example.com  mật khẩu: 12345678
--    9 khách còn lại (quocbao@example.com, thuha@example.com...) dùng chung mật khẩu của khách.
--  Trong bảng users chỉ lưu mật khẩu đã băm bằng BCrypt.
-- =====================================================================

USE scarlet_cinema;
SET NAMES utf8mb4;
SET @hom_nay = CURDATE();

-- Xóa dữ liệu cũ theo thứ tự bảng con trước, bảng cha sau
DELETE FROM tickets;
DELETE FROM bookings;
DELETE FROM promotions;
DELETE FROM showtimes;
DELETE FROM rooms;
DELETE FROM movies;
DELETE FROM users;

-- Đặt lại bộ đếm mã tự tăng, để mã vé và mã đơn đặt vé bắt đầu lại từ đầu mỗi lần nạp
ALTER TABLE tickets   AUTO_INCREMENT = 1;
ALTER TABLE bookings  AUTO_INCREMENT = 1;
ALTER TABLE promotions AUTO_INCREMENT = 1;
ALTER TABLE showtimes AUTO_INCREMENT = 1;
ALTER TABLE rooms     AUTO_INCREMENT = 1;
ALTER TABLE movies    AUTO_INCREMENT = 1;
ALTER TABLE users     AUTO_INCREMENT = 1;

-- 1. Tài khoản: 1 quản trị viên và 10 khách
INSERT INTO users (id, full_name, email, password_hash, role) VALUES
  (1, 'Quản trị viên', 'admin@example.com', '$2a$10$9O5fiZuYGNBoTyNEZ7fOPenPTnNnbjflAa45oPybvVShEEF2QzQIS', 'ADMIN'),
  (2, 'Nguyễn Minh Anh', 'minhanh@example.com', '$2a$10$sY3dCCXPfRLL.piJY4DIzeUF63kpl1NQP/QXUjjsVm12hf9R5wHw6', 'CUSTOMER'),
  (3, 'Trần Quốc Bảo', 'quocbao@example.com', '$2a$10$sY3dCCXPfRLL.piJY4DIzeUF63kpl1NQP/QXUjjsVm12hf9R5wHw6', 'CUSTOMER'),
  (4, 'Lê Thu Hà', 'thuha@example.com', '$2a$10$sY3dCCXPfRLL.piJY4DIzeUF63kpl1NQP/QXUjjsVm12hf9R5wHw6', 'CUSTOMER'),
  (5, 'Phạm Gia Huy', 'giahuy@example.com', '$2a$10$sY3dCCXPfRLL.piJY4DIzeUF63kpl1NQP/QXUjjsVm12hf9R5wHw6', 'CUSTOMER'),
  (6, 'Võ Ngọc Lan', 'ngoclan@example.com', '$2a$10$sY3dCCXPfRLL.piJY4DIzeUF63kpl1NQP/QXUjjsVm12hf9R5wHw6', 'CUSTOMER'),
  (7, 'Đặng Đức Trí', 'ductri@example.com', '$2a$10$sY3dCCXPfRLL.piJY4DIzeUF63kpl1NQP/QXUjjsVm12hf9R5wHw6', 'CUSTOMER'),
  (8, 'Bùi Hoàng Nam', 'hoangnam@example.com', '$2a$10$sY3dCCXPfRLL.piJY4DIzeUF63kpl1NQP/QXUjjsVm12hf9R5wHw6', 'CUSTOMER'),
  (9, 'Đỗ Bích Ngọc', 'bichngoc@example.com', '$2a$10$sY3dCCXPfRLL.piJY4DIzeUF63kpl1NQP/QXUjjsVm12hf9R5wHw6', 'CUSTOMER'),
  (10, 'Phan Tuấn Kiệt', 'tuankiet@example.com', '$2a$10$sY3dCCXPfRLL.piJY4DIzeUF63kpl1NQP/QXUjjsVm12hf9R5wHw6', 'CUSTOMER'),
  (11, 'Lý Mai Phương', 'maiphuong@example.com', '$2a$10$sY3dCCXPfRLL.piJY4DIzeUF63kpl1NQP/QXUjjsVm12hf9R5wHw6', 'CUSTOMER');

-- 2. Phim: 8 phim đang chiếu, 4 phim sắp chiếu (tra ngày 04/10/2026). Phim sắp chiếu chưa công bố thời lượng, độ tuổi thì để NULL.
INSERT INTO movies (id, title, original_title, genre, duration_min, age_rating, release_date, director, description, poster_url, backdrop_url, trailer_url, status) VALUES
  (1, 'Hòn Đảo Quên Lãng', 'Forgotten Island', 'Hoạt hình, phiêu lưu', 109, 'K', '2026-09-25', 'Januel Mercado, Joel Crawford',
   'Đêm cuối trước khi một trong hai phải ra nước ngoài, đôi bạn thân từ thuở nhỏ tình cờ tìm thấy cánh cổng dẫn tới một hòn đảo phép thuật, nơi sinh sống của những sinh vật trong thần thoại Philippines. Họ phải vượt qua hiểm nguy và tìm đường về trước khi mọi ký ức về tình bạn biến mất.',
   'https://cdn.moveek.com/storage/media/cache/tall/6ab2200b001d9061364691.webp', 'https://cdn.moveek.com/storage/media/69ca380153816288909417.jpg',
   'https://www.youtube.com/watch?v=vd1wzfi8-HI', 'NOW_SHOWING'),
  (2, 'Trại Buôn Người', 'The Journey of Heroes', 'Hành động, giật gân', 135, 'T18', '2026-09-25', 'Toni Dương Bảo Anh',
   'Để cứu em gái bị lừa bán qua biên giới, Ny cùng người bạn thân rơi vào sào huyệt của một đường dây lừa đảo và bị ép làm việc như nô lệ. Tại đây họ gặp một trinh sát nằm vùng và cùng nhau lên kế hoạch trốn thoát.',
   'https://cdn.moveek.com/storage/media/cache/tall/6ab9e4ebac84d102988943.webp', 'https://cdn.moveek.com/storage/media/cache/large/6ab9e4f27f7eb775448978.webp',
   'https://www.youtube.com/watch?v=HZXb3P3mitA', 'NOW_SHOWING'),
  (3, 'Quyết Cua Anh Này', 'Henry''s First Dates', 'Hài, tình cảm', 115, 'T13', '2026-10-02', 'Mez Tharatorn',
   'Henry là chàng trai hiền lành, yêu động vật, nhưng mỗi sáng thức dậy anh lại quên hết những gì xảy ra hôm trước. Lucy vì thế phải làm quen và chinh phục anh lại từ đầu mỗi ngày.',
   'https://cdn.moveek.com/storage/media/cache/tall/6ab21df511f52581742502.webp', 'https://cdn.moveek.com/storage/media/6ab21dc1e840c386942482.jpg',
   'https://www.youtube.com/watch?v=XKdEiTx9W68', 'NOW_SHOWING'),
  (4, 'Lên Hương', 'Luck For Sale', 'Tâm lý, gia đình', 121, 'T16', '2026-09-14', 'Tấn Hoàng Thông, Khương Ngọc',
   'Một bà chủ tiệm quan tài đang chật vật giữ nghề và một chàng trai túng thiếu vô tình bước vào một thỏa thuận định mệnh. Từ đó những bí mật bị chôn giấu lâu năm dần lộ ra, khi một người cố giữ kế sinh nhai còn người kia tìm cách cứu mẹ đang bệnh nặng.',
   'https://cdn.moveek.com/storage/media/cache/tall/6a853e8a114e2776669135.webp', 'https://cdn.moveek.com/storage/media/cache/large/6a853e8a30eb9190551086.webp',
   'https://www.youtube.com/watch?v=VcAHmTOK-TI', 'NOW_SHOWING'),
  (5, 'Scotty Giải Cứu Hoàng Thượng', 'Scotty', 'Hoạt hình, gia đình', 80, 'P', '2026-10-02', 'Mohammad Pirzadi',
   'Một chú mèo hoang bất ngờ bị cuốn vào chuyến phiêu lưu cùng một chú mèo nhà. Đến từ hai thế giới khác nhau, cả hai học cách dựa vào nhau và dần hiểu thế nào mới thật sự là nhà.',
   'https://cdn.moveek.com/storage/media/cache/tall/6ab4eb4a27298182568111.webp', NULL,
   'https://www.youtube.com/watch?v=yqYjXjDDthY', 'NOW_SHOWING'),
  (6, 'Út Lan 2', NULL, 'Kinh dị', 107, 'T18', '2026-09-25', 'Trần Trọng Dần',
   'Một con rắn hai đầu xuất hiện, kéo theo hàng loạt cái chết bí ẩn ở ngôi làng ven sông. Khi chuyện cũ bị khơi lại, dân làng nhận ra lời nguyền năm xưa vẫn chưa hề chấm dứt.',
   'https://cdn.moveek.com/storage/media/cache/tall/6aa3a03fbe122916480363.webp', NULL,
   'https://www.youtube.com/watch?v=f9fGXp9yan0', 'NOW_SHOWING'),
  (7, 'Thần Sư Chung Quỳ: Linh Giới Đại Chiến', 'Master Zhong', 'Hoạt hình, giả tưởng', 96, 'T13', '2026-10-02', 'Yuxi Wang, Huang Shan Chuan',
   'Một cô gái người phàm được thợ săn yêu quái huyền thoại cứu mạng rồi theo ông bước vào thế giới yêu quái và xin bái sư. Thầy trò phát hiện một âm mưu có thể phá vỡ thế cân bằng giữa hai thế giới.',
   'https://cdn.moveek.com/storage/media/cache/tall/6ab38c8f95d12597953725.webp', 'https://cdn.moveek.com/storage/media/6ab38c2b78c81246030367.jpg',
   'https://www.youtube.com/watch?v=KEBDVoBTeTk', 'NOW_SHOWING'),
  (8, 'Pháo Hoa Lúc Bình Minh', 'A New Dawn', 'Hoạt hình, tâm lý', 75, 'P', '2026-09-25', 'Yoshitoshi Shinomiya',
   'Xưởng pháo hoa ba trăm năm tuổi của gia đình đứng trước nguy cơ đóng cửa. Ba người trẻ phải đối diện với quá khứ và lựa chọn tương lai giữa truyền thống và cuộc sống hiện đại.',
   'https://cdn.moveek.com/storage/media/cache/tall/6ab2204b0c7e0953056678.webp', 'https://cdn.moveek.com/storage/media/6a98ee91298f2106643730.jpg',
   'https://www.youtube.com/watch?v=oXudBJeF1TI', 'NOW_SHOWING'),
  (9, 'Án Mạng Xém Hoàn Hảo', 'The Almost Perfect Murder', 'Hài, bí ẩn', NULL, NULL, '2026-10-09', 'Đức Nguyễn',
   'Một TikToker tỉnh dậy ở phim trường, mất trí nhớ và nằm cạnh một xác chết. Cùng lúc, một thám tử được thuê điều tra chuyện ngoại tình vô tình dính vào vụ án, và cả hai phải vừa trốn cảnh sát lẫn sát thủ vừa lần ra sự thật phía sau giới giải trí.',
   'https://cdn.moveek.com/storage/media/cache/tall/6a8d5b41107e9384094943.webp', 'https://cdn.moveek.com/storage/media/cache/large/6a8d5b4137e8c939819537.webp',
   'https://www.youtube.com/watch?v=IW-k9cMySWg', 'COMING_SOON'),
  (10, 'Blue Lock: Cuộc Chiến Của Những Tiền Đạo', 'Blue Lock', 'Hành động, tâm lý', 128, NULL, '2026-10-09', 'Yusuke Taki',
   'Sau nhiều năm thất bại, Liên đoàn bóng đá Nhật Bản mở một chương trình huấn luyện bí mật để tìm ra tiền đạo số một thế giới. Ba trăm cầu thủ trung học bị đưa vào một cơ sở biệt lập, nơi chỉ một người trụ lại cuối cùng.',
   'https://cdn.moveek.com/storage/media/cache/tall/6abb6311dd730152956643.webp', 'https://cdn.moveek.com/storage/media/6abb62dd10106423857845.jpg',
   'https://www.youtube.com/watch?v=y4_4y7VaSTM', 'COMING_SOON'),
  (11, 'Street Fighter', 'Street Fighter', 'Hành động, giả tưởng', NULL, NULL, '2026-10-16', 'Kitao Sakurai',
   'Năm 1993, hai võ sĩ Ryu và Ken được Chun-Li chiêu mộ tham gia giải đấu Chiến Binh Thế Giới. Phía sau giải đấu là một âm mưu nguy hiểm buộc họ đối đầu với nhau và với quá khứ của chính mình.',
   'https://cdn.moveek.com/storage/media/cache/tall/6a538ba804bfe303915257.webp', 'https://cdn.moveek.com/storage/media/6a538ba81c7f6196807171.jpg',
   'https://www.youtube.com/watch?v=Xt4X4FvXk2A', 'COMING_SOON'),
  (12, 'Mẹ Mìn', NULL, 'Tâm lý', NULL, NULL, '2026-10-23', 'Jack Carry On',
   NULL,
   'https://cdn.moveek.com/storage/media/cache/tall/6aab7c283907e020377678.webp', NULL,
   NULL, 'COMING_SOON');

-- 3. Phòng chiếu: 3 phòng, mỗi phòng 8 hàng x 10 ghế = 80 ghế
INSERT INTO rooms (id, name, seat_rows, seats_per_row) VALUES
  (1, 'Phòng 1', 8, 10),
  (2, 'Phòng 2', 8, 10),
  (3, 'Phòng 3', 8, 10);

-- 4. Suất chiếu
-- 4a. Ba suất đã chiếu, để tài khoản Minh Anh có lịch sử vé
INSERT INTO showtimes (id, movie_id, room_id, start_time, price) VALUES
  (1, 8, 1, TIMESTAMP(@hom_nay - INTERVAL 7 DAY,  '10:30:00'), 60000),
  (2, 2, 2, TIMESTAMP(@hom_nay - INTERVAL 9 DAY,  '21:45:00'), 75000),
  (3, 4, 1, TIMESTAMP(@hom_nay - INTERVAL 20 DAY, '16:00:00'), 75000);

-- 4b. Khung lịch một ngày: 3 phòng x 5 khung giờ. Cột thu_tu dùng để đánh mã suất chiếu.
CREATE TEMPORARY TABLE khung_lich (thu_tu INT PRIMARY KEY, room_id INT, gio TIME, movie_id INT);
INSERT INTO khung_lich VALUES
  (1, 1, '10:30:00', 1), (2, 1, '13:15:00', 5), (3, 1, '16:00:00', 3), (4, 1, '19:30:00', 6), (5, 1, '21:45:00', 2),
  (6, 2, '10:30:00', 7), (7, 2, '13:15:00', 8), (8, 2, '16:00:00', 4), (9, 2, '19:30:00', 3), (10, 2, '21:45:00', 6),
  (11, 3, '10:30:00', 5), (12, 3, '13:15:00', 1), (13, 3, '16:00:00', 7), (14, 3, '19:30:00', 1), (15, 3, '21:45:00', 4);

CREATE TEMPORARY TABLE ngay_chieu (n INT PRIMARY KEY);
INSERT INTO ngay_chieu VALUES (0), (1), (2), (3), (4), (5), (6);

-- 4c. Nhân khung lịch với 7 ngày. Giá vé: thứ Ba đồng giá 45.000 đ, trước 16:00 là 60.000 đ, từ 16:00 là 75.000 đ.
--     Mã suất chiếu = 100 + (số ngày tính từ hôm nay) x 15 + thứ tự trong ngày.
INSERT INTO showtimes (id, movie_id, room_id, start_time, price)
SELECT 100 + d.n * 15 + k.thu_tu,
       k.movie_id,
       k.room_id,
       TIMESTAMP(@hom_nay + INTERVAL d.n DAY, k.gio),
       CASE WHEN DAYOFWEEK(@hom_nay + INTERVAL d.n DAY) = 3 THEN 45000
            WHEN k.gio < '16:00:00' THEN 60000
            ELSE 75000 END
FROM ngay_chieu d CROSS JOIN khung_lich k
ORDER BY d.n, k.thu_tu;

DROP TEMPORARY TABLE khung_lich;
DROP TEMPORARY TABLE ngay_chieu;

-- 5. Mã ưu đãi: bốn mã đang dùng được (hiện trên trang chủ), một mã đã hết hạn và một mã đã hết lượt để thử.
--    Ngày dùng được tính theo ngày chạy file, giống lịch chiếu.
INSERT INTO promotions (id, code, title, description, discount_type, discount_value, max_discount, min_total, start_date, end_date, usage_limit, active) VALUES
  (1, 'SCARLET10',  'Giảm 10% mọi đơn',   'Giảm 10% tiền vé, tối đa 30.000 đ. Dùng được cho mọi đơn.',        'PERCENT', 10, 30000,      0, @hom_nay - INTERVAL 30 DAY, @hom_nay + INTERVAL 60 DAY, NULL, 1),
  (2, 'BANMOI',     'Chào bạn mới',       'Giảm 20.000 đ cho đơn từ 100.000 đ.',                              'AMOUNT',  20000, NULL, 100000, @hom_nay - INTERVAL 30 DAY, @hom_nay + INTERVAL 60 DAY, NULL, 1),
  (3, 'DOIBAN',     'Đi xem cùng bạn',    'Giảm 30.000 đ cho đơn từ 150.000 đ.',                              'AMOUNT',  30000, NULL, 150000, @hom_nay - INTERVAL 30 DAY, @hom_nay + INTERVAL 60 DAY, NULL, 1),
  (4, 'PHIMHAY',    'Tuần phim hay',      'Giảm 15% tiền vé, tối đa 50.000 đ, cho đơn từ 200.000 đ.',         'PERCENT', 15, 50000, 200000, @hom_nay - INTERVAL 7 DAY,  @hom_nay + INTERVAL 14 DAY, 200,  1),
  (5, 'HE2026',     'Ưu đãi mùa hè',      'Giảm 20% tiền vé, tối đa 40.000 đ.',                               'PERCENT', 20, 40000,      0, @hom_nay - INTERVAL 120 DAY, @hom_nay - INTERVAL 30 DAY, NULL, 1),
  (6, 'KHAITRUONG', 'Mừng khai trương',   'Giảm 25.000 đ cho 3 đơn đầu tiên.',                                'AMOUNT',  25000, NULL,      0, @hom_nay - INTERVAL 30 DAY, @hom_nay + INTERVAL 60 DAY, 3,    1);

-- 6. Đơn đặt vé (134 đơn), tất cả đã thanh toán (trả sau lúc đặt 1 đến 6 phút).
--    Mã đặt vé và tổng tiền được tính lại ở mục 8, ở đây ghi tạm.
INSERT INTO bookings (id, booking_code, user_id, showtime_id, total_amount, status, payment_method, created_at, paid_at) VALUES
  (1, 'TAM-1', 11, 103, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '08:00:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '08:00:00') + INTERVAL 2 MINUTE),
  (2, 'TAM-2', 7, 101, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '08:39:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '08:39:00') + INTERVAL 3 MINUTE),
  (3, 'TAM-3', 4, 105, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '09:19:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '09:19:00') + INTERVAL 4 MINUTE),
  (4, 'TAM-4', 9, 103, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '09:58:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '09:58:00') + INTERVAL 5 MINUTE),
  (5, 'TAM-5', 7, 105, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '10:38:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '10:38:00') + INTERVAL 6 MINUTE),
  (6, 'TAM-6', 10, 107, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '11:17:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '11:17:00') + INTERVAL 1 MINUTE),
  (7, 'TAM-7', 8, 106, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '11:57:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '11:57:00') + INTERVAL 2 MINUTE),
  (8, 'TAM-8', 6, 109, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '12:36:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '12:36:00') + INTERVAL 3 MINUTE),
  (9, 'TAM-9', 6, 109, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '13:16:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '13:16:00') + INTERVAL 4 MINUTE),
  (10, 'TAM-10', 5, 103, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '13:55:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '13:55:00') + INTERVAL 5 MINUTE),
  (11, 'TAM-11', 8, 105, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '14:35:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '14:35:00') + INTERVAL 6 MINUTE),
  (12, 'TAM-12', 10, 103, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '15:14:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '15:14:00') + INTERVAL 1 MINUTE),
  (13, 'TAM-13', 4, 103, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '15:54:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '15:54:00') + INTERVAL 2 MINUTE),
  (14, 'TAM-14', 5, 108, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '16:33:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '16:33:00') + INTERVAL 3 MINUTE),
  (15, 'TAM-15', 7, 102, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '17:13:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '17:13:00') + INTERVAL 4 MINUTE),
  (16, 'TAM-16', 11, 112, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '17:52:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '17:52:00') + INTERVAL 5 MINUTE),
  (17, 'TAM-17', 3, 103, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '18:32:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '18:32:00') + INTERVAL 6 MINUTE),
  (18, 'TAM-18', 11, 108, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '19:11:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '19:11:00') + INTERVAL 1 MINUTE),
  (19, 'TAM-19', 8, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '19:51:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '19:51:00') + INTERVAL 2 MINUTE),
  (20, 'TAM-20', 6, 112, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '20:30:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '20:30:00') + INTERVAL 3 MINUTE),
  (21, 'TAM-21', 7, 105, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '21:10:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '21:10:00') + INTERVAL 4 MINUTE),
  (22, 'TAM-22', 10, 104, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '21:49:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '21:49:00') + INTERVAL 5 MINUTE),
  (23, 'TAM-23', 3, 111, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '22:29:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '22:29:00') + INTERVAL 6 MINUTE),
  (24, 'TAM-24', 7, 109, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '12:08:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '12:08:00') + INTERVAL 1 MINUTE),
  (25, 'TAM-25', 5, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '12:48:00'), TIMESTAMP(@hom_nay - INTERVAL 5 DAY, '12:48:00') + INTERVAL 2 MINUTE),
  (26, 'TAM-26', 7, 115, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '09:27:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '09:27:00') + INTERVAL 3 MINUTE),
  (27, 'TAM-27', 10, 105, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '10:07:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '10:07:00') + INTERVAL 4 MINUTE),
  (28, 'TAM-28', 9, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '10:46:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '10:46:00') + INTERVAL 5 MINUTE),
  (29, 'TAM-29', 4, 103, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '11:26:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '11:26:00') + INTERVAL 6 MINUTE),
  (30, 'TAM-30', 6, 107, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '12:05:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '12:05:00') + INTERVAL 1 MINUTE),
  (31, 'TAM-31', 7, 101, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '12:45:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '12:45:00') + INTERVAL 2 MINUTE),
  (32, 'TAM-32', 4, 105, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '13:24:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '13:24:00') + INTERVAL 3 MINUTE),
  (33, 'TAM-33', 7, 103, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '14:04:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '14:04:00') + INTERVAL 4 MINUTE),
  (34, 'TAM-34', 9, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '14:43:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '14:43:00') + INTERVAL 5 MINUTE),
  (35, 'TAM-35', 4, 105, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '15:23:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '15:23:00') + INTERVAL 6 MINUTE),
  (36, 'TAM-36', 10, 101, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '07:02:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '07:02:00') + INTERVAL 1 MINUTE),
  (37, 'TAM-37', 8, 110, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '07:42:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '07:42:00') + INTERVAL 2 MINUTE),
  (38, 'TAM-38', 7, 101, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '08:21:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '08:21:00') + INTERVAL 3 MINUTE),
  (39, 'TAM-39', 7, 107, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '09:01:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '09:01:00') + INTERVAL 4 MINUTE),
  (40, 'TAM-40', 5, 109, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '09:40:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '09:40:00') + INTERVAL 5 MINUTE),
  (41, 'TAM-41', 3, 105, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '10:20:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '10:20:00') + INTERVAL 6 MINUTE),
  (42, 'TAM-42', 4, 110, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '10:59:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '10:59:00') + INTERVAL 1 MINUTE),
  (43, 'TAM-43', 7, 103, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '11:39:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '11:39:00') + INTERVAL 2 MINUTE),
  (44, 'TAM-44', 9, 105, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '12:18:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '12:18:00') + INTERVAL 3 MINUTE),
  (45, 'TAM-45', 4, 109, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '12:58:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '12:58:00') + INTERVAL 4 MINUTE),
  (46, 'TAM-46', 6, 107, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '13:37:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '13:37:00') + INTERVAL 5 MINUTE),
  (47, 'TAM-47', 6, 113, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '14:17:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '14:17:00') + INTERVAL 6 MINUTE),
  (48, 'TAM-48', 11, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '14:56:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '14:56:00') + INTERVAL 1 MINUTE),
  (49, 'TAM-49', 3, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '15:36:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '15:36:00') + INTERVAL 2 MINUTE),
  (50, 'TAM-50', 3, 104, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '16:15:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '16:15:00') + INTERVAL 3 MINUTE),
  (51, 'TAM-51', 2, 3, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 21 DAY, '20:15:00'), TIMESTAMP(@hom_nay - INTERVAL 21 DAY, '20:15:00') + INTERVAL 4 MINUTE),
  (52, 'TAM-52', 8, 107, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '16:55:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '16:55:00') + INTERVAL 5 MINUTE),
  (53, 'TAM-53', 6, 113, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '17:34:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '17:34:00') + INTERVAL 6 MINUTE),
  (54, 'TAM-54', 3, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '18:14:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '18:14:00') + INTERVAL 1 MINUTE),
  (55, 'TAM-55', 8, 104, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '18:53:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '18:53:00') + INTERVAL 2 MINUTE),
  (56, 'TAM-56', 4, 105, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '19:33:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '19:33:00') + INTERVAL 3 MINUTE),
  (57, 'TAM-57', 7, 103, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '20:12:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '20:12:00') + INTERVAL 4 MINUTE),
  (58, 'TAM-58', 10, 103, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '20:52:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '20:52:00') + INTERVAL 5 MINUTE),
  (59, 'TAM-59', 11, 103, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '21:31:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '21:31:00') + INTERVAL 6 MINUTE),
  (60, 'TAM-60', 9, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '22:11:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '22:11:00') + INTERVAL 1 MINUTE),
  (61, 'TAM-61', 8, 113, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '22:50:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '22:50:00') + INTERVAL 2 MINUTE),
  (62, 'TAM-62', 11, 105, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '12:30:00'), TIMESTAMP(@hom_nay - INTERVAL 4 DAY, '12:30:00') + INTERVAL 3 MINUTE),
  (63, 'TAM-63', 6, 105, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '09:09:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '09:09:00') + INTERVAL 4 MINUTE),
  (64, 'TAM-64', 3, 111, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '09:49:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '09:49:00') + INTERVAL 5 MINUTE),
  (65, 'TAM-65', 8, 106, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '10:28:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '10:28:00') + INTERVAL 6 MINUTE),
  (66, 'TAM-66', 10, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '11:08:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '11:08:00') + INTERVAL 1 MINUTE),
  (67, 'TAM-67', 10, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '11:47:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '11:47:00') + INTERVAL 2 MINUTE),
  (68, 'TAM-68', 9, 107, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '12:27:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '12:27:00') + INTERVAL 3 MINUTE),
  (69, 'TAM-69', 3, 103, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '13:06:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '13:06:00') + INTERVAL 4 MINUTE),
  (70, 'TAM-70', 8, 107, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '13:46:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '13:46:00') + INTERVAL 5 MINUTE),
  (71, 'TAM-71', 8, 103, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '14:25:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '14:25:00') + INTERVAL 6 MINUTE),
  (72, 'TAM-72', 6, 108, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '15:05:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '15:05:00') + INTERVAL 1 MINUTE),
  (73, 'TAM-73', 11, 108, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '15:44:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '15:44:00') + INTERVAL 2 MINUTE),
  (74, 'TAM-74', 7, 103, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '07:24:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '07:24:00') + INTERVAL 3 MINUTE),
  (75, 'TAM-75', 11, 102, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '08:03:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '08:03:00') + INTERVAL 4 MINUTE),
  (76, 'TAM-76', 11, 109, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '08:43:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '08:43:00') + INTERVAL 5 MINUTE),
  (77, 'TAM-77', 8, 105, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '09:22:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '09:22:00') + INTERVAL 6 MINUTE),
  (78, 'TAM-78', 9, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '10:02:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '10:02:00') + INTERVAL 1 MINUTE),
  (79, 'TAM-79', 11, 103, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '10:41:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '10:41:00') + INTERVAL 2 MINUTE),
  (80, 'TAM-80', 4, 101, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '11:21:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '11:21:00') + INTERVAL 3 MINUTE),
  (81, 'TAM-81', 3, 105, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '12:00:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '12:00:00') + INTERVAL 4 MINUTE),
  (82, 'TAM-82', 9, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '12:40:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '12:40:00') + INTERVAL 5 MINUTE),
  (83, 'TAM-83', 10, 107, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '13:19:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '13:19:00') + INTERVAL 6 MINUTE),
  (84, 'TAM-84', 3, 101, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '13:59:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '13:59:00') + INTERVAL 1 MINUTE),
  (85, 'TAM-85', 3, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '14:38:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '14:38:00') + INTERVAL 2 MINUTE),
  (86, 'TAM-86', 10, 105, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '15:18:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '15:18:00') + INTERVAL 3 MINUTE),
  (87, 'TAM-87', 5, 112, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '15:57:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '15:57:00') + INTERVAL 4 MINUTE),
  (88, 'TAM-88', 2, 2, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 10 DAY, '12:40:00'), TIMESTAMP(@hom_nay - INTERVAL 10 DAY, '12:40:00') + INTERVAL 5 MINUTE),
  (89, 'TAM-89', 10, 108, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '16:37:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '16:37:00') + INTERVAL 6 MINUTE),
  (90, 'TAM-90', 9, 101, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '17:16:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '17:16:00') + INTERVAL 1 MINUTE),
  (91, 'TAM-91', 4, 106, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '17:56:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '17:56:00') + INTERVAL 2 MINUTE),
  (92, 'TAM-92', 8, 107, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '18:35:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '18:35:00') + INTERVAL 3 MINUTE),
  (93, 'TAM-93', 4, 115, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '19:15:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '19:15:00') + INTERVAL 4 MINUTE),
  (94, 'TAM-94', 7, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '19:54:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '19:54:00') + INTERVAL 5 MINUTE),
  (95, 'TAM-95', 4, 110, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '20:34:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '20:34:00') + INTERVAL 6 MINUTE),
  (96, 'TAM-96', 6, 107, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '21:13:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '21:13:00') + INTERVAL 1 MINUTE),
  (97, 'TAM-97', 9, 103, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '21:53:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '21:53:00') + INTERVAL 2 MINUTE),
  (98, 'TAM-98', 6, 112, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '22:32:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '22:32:00') + INTERVAL 3 MINUTE),
  (99, 'TAM-99', 6, 111, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '12:12:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '12:12:00') + INTERVAL 4 MINUTE),
  (100, 'TAM-100', 7, 102, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '12:51:00'), TIMESTAMP(@hom_nay - INTERVAL 3 DAY, '12:51:00') + INTERVAL 5 MINUTE),
  (101, 'TAM-101', 6, 105, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '09:31:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '09:31:00') + INTERVAL 6 MINUTE),
  (102, 'TAM-102', 2, 1, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 8 DAY, '19:05:00'), TIMESTAMP(@hom_nay - INTERVAL 8 DAY, '19:05:00') + INTERVAL 1 MINUTE),
  (103, 'TAM-103', 10, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '10:10:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '10:10:00') + INTERVAL 2 MINUTE),
  (104, 'TAM-104', 3, 105, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '10:50:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '10:50:00') + INTERVAL 3 MINUTE),
  (105, 'TAM-105', 4, 104, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '11:29:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '11:29:00') + INTERVAL 4 MINUTE),
  (106, 'TAM-106', 3, 107, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '12:09:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '12:09:00') + INTERVAL 5 MINUTE),
  (107, 'TAM-107', 11, 105, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '12:48:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '12:48:00') + INTERVAL 6 MINUTE),
  (108, 'TAM-108', 10, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '13:28:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '13:28:00') + INTERVAL 1 MINUTE),
  (109, 'TAM-109', 8, 102, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '14:07:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '14:07:00') + INTERVAL 2 MINUTE),
  (110, 'TAM-110', 5, 105, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '14:47:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '14:47:00') + INTERVAL 3 MINUTE),
  (111, 'TAM-111', 9, 115, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '15:26:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '15:26:00') + INTERVAL 4 MINUTE),
  (112, 'TAM-112', 5, 101, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '07:06:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '07:06:00') + INTERVAL 5 MINUTE),
  (113, 'TAM-113', 6, 103, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '07:45:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '07:45:00') + INTERVAL 6 MINUTE),
  (114, 'TAM-114', 7, 103, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '08:25:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '08:25:00') + INTERVAL 1 MINUTE),
  (115, 'TAM-115', 5, 101, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '09:04:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '09:04:00') + INTERVAL 2 MINUTE),
  (116, 'TAM-116', 7, 101, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '09:44:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '09:44:00') + INTERVAL 3 MINUTE),
  (117, 'TAM-117', 9, 103, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '10:23:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '10:23:00') + INTERVAL 4 MINUTE),
  (118, 'TAM-118', 7, 103, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '11:03:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '11:03:00') + INTERVAL 5 MINUTE),
  (119, 'TAM-119', 9, 101, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '11:42:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '11:42:00') + INTERVAL 6 MINUTE),
  (120, 'TAM-120', 9, 103, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '12:22:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '12:22:00') + INTERVAL 1 MINUTE),
  (121, 'TAM-121', 11, 106, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '13:01:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '13:01:00') + INTERVAL 2 MINUTE),
  (122, 'TAM-122', 9, 112, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '13:41:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '13:41:00') + INTERVAL 3 MINUTE),
  (123, 'TAM-123', 6, 115, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '14:20:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '14:20:00') + INTERVAL 4 MINUTE),
  (137, 'TAM-137', 8, 114, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '16:10:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '16:10:00') + INTERVAL 6 MINUTE),
  (138, 'TAM-138', 7, 114, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '16:21:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '16:21:00') + INTERVAL 1 MINUTE),
  (139, 'TAM-139', 6, 114, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '16:33:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '16:33:00') + INTERVAL 2 MINUTE),
  (140, 'TAM-140', 5, 114, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '16:42:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '16:42:00') + INTERVAL 3 MINUTE),
  (141, 'TAM-141', 4, 114, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '16:50:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '16:50:00') + INTERVAL 4 MINUTE),
  (142, 'TAM-142', 7, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '17:02:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '17:02:00') + INTERVAL 5 MINUTE),
  (143, 'TAM-143', 6, 103, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '18:27:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '18:27:00') + INTERVAL 6 MINUTE),
  (144, 'TAM-144', 5, 105, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '20:03:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '20:03:00') + INTERVAL 1 MINUTE),
  (145, 'TAM-145', 4, 107, 0, 'PAID', 'BANK_QR', TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '21:40:00'), TIMESTAMP(@hom_nay - INTERVAL 2 DAY, '21:40:00') + INTERVAL 2 MINUTE),
  (146, 'TAM-146', 3, 114, 0, 'PAID', 'EWALLET', TIMESTAMP(@hom_nay - INTERVAL 1 DAY, '08:55:00'), TIMESTAMP(@hom_nay - INTERVAL 1 DAY, '08:55:00') + INTERVAL 3 MINUTE),
  (147, 'TAM-147', 2, 114, 0, 'PAID', 'CARD', TIMESTAMP(@hom_nay - INTERVAL 1 DAY, '09:12:00'), TIMESTAMP(@hom_nay - INTERVAL 1 DAY, '09:12:00') + INTERVAL 4 MINUTE);

-- 7. Vé (287 vé): mỗi dòng là một ghế của một đơn. Suất chiếu và giá vé lấy theo đơn đặt vé.
CREATE TEMPORARY TABLE ghe_da_dat (booking_id INT, seat_code VARCHAR(4), PRIMARY KEY (booking_id, seat_code));
INSERT INTO ghe_da_dat VALUES
  (1, 'C3'), (1, 'C4'), (2, 'B8'), (2, 'B9'), (3, 'E1'), (3, 'E2'), (4, 'B1'), (4, 'B2'),
  (5, 'B9'), (6, 'E7'), (6, 'E8'), (6, 'E9'), (7, 'C7'), (7, 'C8'), (8, 'C9'), (8, 'C10'),
  (9, 'E1'), (9, 'E2'), (10, 'A5'), (10, 'A6'), (10, 'A7'), (11, 'D1'), (12, 'C5'), (12, 'C6'),
  (12, 'C7'), (13, 'A8'), (13, 'A9'), (13, 'A10'), (14, 'C10'), (15, 'H8'), (16, 'B6'), (16, 'B7'),
  (16, 'B8'), (17, 'H6'), (17, 'H7'), (18, 'E5'), (18, 'E6'), (18, 'E7'), (19, 'B3'), (20, 'A6'),
  (20, 'A7'), (21, 'C1'), (21, 'C2'), (21, 'C3'), (22, 'G7'), (22, 'G8'), (22, 'G9'), (22, 'G10'),
  (23, 'B4'), (23, 'B5'), (24, 'G5'), (24, 'G6'), (25, 'G4'), (25, 'G5'), (26, 'A5'), (27, 'B1'),
  (27, 'B2'), (28, 'G1'), (28, 'G2'), (28, 'G3'), (29, 'E2'), (30, 'F6'), (31, 'G8'), (31, 'G9'),
  (31, 'G10'), (32, 'G9'), (33, 'F7'), (33, 'F8'), (34, 'C6'), (34, 'C7'), (35, 'A6'), (35, 'A7'),
  (35, 'A8'), (36, 'B4'), (36, 'B5'), (36, 'B6'), (37, 'E4'), (37, 'E5'), (37, 'E6'), (38, 'C5'),
  (38, 'C6'), (39, 'G5'), (39, 'G6'), (39, 'G7'), (40, 'E9'), (41, 'A9'), (41, 'A10'), (42, 'H3'),
  (42, 'H4'), (42, 'H5'), (43, 'F10'), (44, 'A5'), (45, 'B4'), (45, 'B5'), (45, 'B6'), (45, 'B7'),
  (46, 'G2'), (46, 'G3'), (47, 'F5'), (47, 'F6'), (48, 'F3'), (48, 'F4'), (49, 'H1'), (49, 'H2'),
  (50, 'B5'), (50, 'B6'), (50, 'B7'), (50, 'B8'), (51, 'D5'), (51, 'D6'), (52, 'B7'), (52, 'B8'),
  (52, 'B9'), (53, 'G8'), (54, 'F9'), (54, 'F10'), (55, 'B1'), (55, 'B2'), (56, 'H5'), (56, 'H6'),
  (56, 'H7'), (56, 'H8'), (57, 'C1'), (57, 'C2'), (58, 'G3'), (58, 'G4'), (58, 'G5'), (59, 'H10'),
  (60, 'C10'), (61, 'H1'), (61, 'H2'), (61, 'H3'), (61, 'H4'), (62, 'H10'), (63, 'C4'), (63, 'C5'),
  (64, 'D10'), (65, 'H8'), (66, 'H9'), (67, 'B5'), (67, 'B6'), (67, 'B7'), (68, 'E1'), (69, 'D8'),
  (69, 'D9'), (69, 'D10'), (70, 'C8'), (70, 'C9'), (71, 'D2'), (71, 'D3'), (71, 'D4'), (72, 'D7'),
  (72, 'D8'), (72, 'D9'), (73, 'G2'), (73, 'G3'), (73, 'G4'), (74, 'E8'), (74, 'E9'), (75, 'C2'),
  (75, 'C3'), (75, 'C4'), (76, 'D6'), (76, 'D7'), (76, 'D8'), (77, 'E4'), (77, 'E5'), (77, 'E6'),
  (78, 'E7'), (78, 'E8'), (79, 'B7'), (80, 'H2'), (80, 'H3'), (80, 'H4'), (80, 'H5'), (81, 'A4'),
  (82, 'D8'), (82, 'D9'), (83, 'A6'), (83, 'A7'), (83, 'A8'), (83, 'A9'), (84, 'B2'), (85, 'F5'),
  (85, 'F6'), (85, 'F7'), (86, 'B10'), (87, 'G2'), (87, 'G3'), (87, 'G4'), (88, 'F7'), (89, 'F3'),
  (90, 'F7'), (90, 'F8'), (91, 'B1'), (91, 'B2'), (92, 'H7'), (92, 'H8'), (93, 'F4'), (93, 'F5'),
  (93, 'F6'), (93, 'F7'), (94, 'A2'), (95, 'E7'), (95, 'E8'), (96, 'D3'), (96, 'D4'), (96, 'D5'),
  (97, 'B8'), (97, 'B9'), (98, 'F6'), (98, 'F7'), (98, 'F8'), (99, 'E5'), (99, 'E6'), (100, 'F4'),
  (101, 'D5'), (101, 'D6'), (101, 'D7'), (102, 'C3'), (102, 'C4'), (103, 'E9'), (103, 'E10'), (104, 'B4'),
  (105, 'H7'), (105, 'H8'), (106, 'H2'), (106, 'H3'), (107, 'C9'), (108, 'H3'), (108, 'H4'), (109, 'H3'),
  (109, 'H4'), (109, 'H5'), (109, 'H6'), (110, 'D2'), (110, 'D3'), (110, 'D4'), (111, 'G5'), (111, 'G6'),
  (112, 'H6'), (113, 'C10'), (114, 'A2'), (114, 'A3'), (115, 'E5'), (116, 'G5'), (116, 'G6'), (117, 'G8'),
  (117, 'G9'), (118, 'H2'), (118, 'H3'), (119, 'F9'), (120, 'F2'), (120, 'F3'), (120, 'F4'), (120, 'F5'),
  (121, 'B5'), (122, 'G8'), (122, 'G9'), (123, 'A2'), (123, 'A3'), (123, 'A4'), (137, 'G6'), (137, 'G7'),
  (137, 'G8'), (138, 'F1'), (138, 'F2'), (139, 'E8'), (140, 'C6'), (140, 'C7'), (141, 'B4'), (141, 'B5'),
  (142, 'G6'), (142, 'G7'), (142, 'G8'), (143, 'B4'), (143, 'B5'), (144, 'F1'), (144, 'F2'), (145, 'C6'),
  (145, 'C7'), (146, 'D3'), (146, 'D4'), (146, 'D5'), (147, 'E4'), (147, 'E5'), (147, 'E6');

INSERT INTO tickets (booking_id, showtime_id, seat_code, price)
SELECT g.booking_id, b.showtime_id, g.seat_code, s.price
FROM ghe_da_dat g
JOIN bookings  b ON b.id = g.booking_id
JOIN showtimes s ON s.id = b.showtime_id
ORDER BY g.booking_id, g.seat_code;

DROP TEMPORARY TABLE ghe_da_dat;

-- 8. Tính tổng tiền từng đơn và đặt mã đặt vé theo dạng SC-<ngày chiếu>-<mã đơn 4 chữ số>
UPDATE bookings b
JOIN showtimes s ON s.id = b.showtime_id
SET b.total_amount = (SELECT SUM(t.price) FROM tickets t WHERE t.booking_id = b.id),
    b.booking_code = CONCAT('SC-', DATE_FORMAT(s.start_time, '%Y%m%d'), '-', LPAD(b.id, 4, '0'));

-- 9. Một số đơn đã dùng mã ưu đãi. Trong lệnh UPDATE một bảng, MySQL gán lần lượt từ trái sang phải:
--    tính số tiền giảm từ tiền vé gốc trước, rồi mới trừ vào số tiền phải trả.
UPDATE bookings SET promotion_id = 1, discount_amount = LEAST(ROUND(total_amount * 0.10), 30000), total_amount = total_amount - discount_amount
WHERE id IN (17, 10, 2, 4, 1);
UPDATE bookings SET promotion_id = 2, discount_amount = 20000, total_amount = total_amount - discount_amount
WHERE id IN (13, 72, 37, 6) AND total_amount >= 100000;
UPDATE bookings SET promotion_id = 3, discount_amount = 30000, total_amount = total_amount - discount_amount
WHERE id IN (50, 120) AND total_amount >= 150000;
UPDATE bookings SET promotion_id = 6, discount_amount = 25000, total_amount = total_amount - discount_amount
WHERE id IN (14, 5, 12);

-- 10. Kiểm tra nhanh số dòng từng bảng
SELECT 'users' AS bang, COUNT(*) AS so_dong FROM users
UNION ALL SELECT 'movies', COUNT(*) FROM movies
UNION ALL SELECT 'rooms', COUNT(*) FROM rooms
UNION ALL SELECT 'showtimes', COUNT(*) FROM showtimes
UNION ALL SELECT 'promotions', COUNT(*) FROM promotions
UNION ALL SELECT 'bookings', COUNT(*) FROM bookings
UNION ALL SELECT 'tickets', COUNT(*) FROM tickets;
