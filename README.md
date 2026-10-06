# Scarlet Cinema

Website quản lý rạp chiếu phim và đặt vé trực tuyến. Khách xem lịch chiếu, chọn ghế trên sơ đồ phòng, giữ ghế, thanh toán và nhận vé có mã QR. Quản trị viên quản lý phim, suất chiếu và vé đã đặt.

## Chức năng

**Khách**

- Trang chủ: phim nổi bật, các suất sắp chiếu kèm số ghế còn, đặt vé nhanh, phim đang chiếu (lọc theo thể loại, tìm theo tên), phim sắp chiếu, các mã ưu đãi đang dùng được.
- Chi tiết phim và lịch chiếu 7 ngày tới.
- Xem trailer ngay trên web: bấm "Xem trailer" thì video phát trong một khung trên trang, không phải chuyển sang YouTube.
- Đăng ký, đăng nhập, đăng xuất. Mật khẩu được băm bằng BCrypt.
- Đặt vé: chọn tối đa 8 ghế mỗi lần, ghế được giữ 10 phút, thanh toán xong mới có vé mã QR. Quá hạn hoặc hủy đơn thì ghế được mở bán lại.
- Mã ưu đãi: nhập mã ở bước thanh toán để được giảm theo phần trăm hoặc một số tiền cố định. Mỗi đơn dùng một mã, mỗi tài khoản dùng mỗi mã một lần; mã có hạn dùng, có thể có mức đơn tối thiểu và số lượt tối đa.
- Lịch sử vé: đơn chờ thanh toán và vé đã mua.
- Trang thông tin: giới thiệu, phòng chiếu, quy định đặt vé, phân loại độ tuổi, hỏi đáp, chính sách bảo mật.

**Quản trị viên**

- Thêm, sửa, xóa phim; mỗi phim có thể kèm đường dẫn trailer trên YouTube.
- Thêm, sửa, xóa suất chiếu; web chặn hai suất trùng giờ trong cùng một phòng.
- Danh sách vé đã đặt, tìm theo mã vé hoặc tên khách, tổng doanh thu và doanh thu theo phim.
- Tình hình dùng mã ưu đãi: số lượt đã dùng, tổng tiền đã giảm, mã nào hết hạn hay hết lượt.
- Tài khoản quản trị chỉ dùng để quản lý, không đặt vé: bấm đặt vé thì web nhắc đăng nhập bằng tài khoản khách.

Bước thanh toán đang ở chế độ thử nghiệm: web không kết nối ngân hàng và không trừ tiền thật.

## Công nghệ

| Phần | Dùng |
| --- | --- |
| Ngôn ngữ | Java 17 trở lên |
| Khung web | Spring Boot 3.5.6 (Spring MVC, máy chủ Tomcat nhúng) |
| Giao diện | Thymeleaf, CSS viết tay, Bootstrap 5.3 cho hộp thoại |
| Cơ sở dữ liệu | MySQL 8 hoặc MariaDB 10.4, truy cập bằng JDBC (JdbcTemplate) với câu lệnh SQL viết tay |
| Thư viện khác | spring-security-crypto (BCrypt), ZXing (mã QR), Spring Session JDBC (lưu phiên đăng nhập trong MySQL) |
| Công cụ build | Maven |

## Cấu trúc thư mục

```
pom.xml                     Khai báo dự án Maven và thư viện
Dockerfile.vercel           Cách đóng gói web để chạy trên Vercel (trên máy cá nhân không cần)
vercel.json                 Báo cho Vercel chạy web dưới dạng container theo Dockerfile.vercel
chay-web.bat                Bấm đúp để build và chạy web trên Windows
database/                   File SQL: tạo bảng, dữ liệu mẫu, truy vấn mẫu, kiểm tra ràng buộc, sơ đồ bảng
src/main/java/vn/scarlet/cinema/
    DatabaseCheck, DatabaseUpgrade   Lúc khởi động: báo tình trạng kết nối MySQL; cơ sở dữ liệu trống thì tự tạo bảng và nạp dữ liệu mẫu,
                                     cơ sở dữ liệu cũ thì tự thêm cột còn thiếu
    model/                  Các lớp dữ liệu (Movie, Room, ShowtimeView, BookingView, Promotion...)
    dao/                    Nơi viết câu lệnh SQL cho từng bảng
    service/                Nghiệp vụ đặt vé (giữ ghế trong một giao dịch, áp mã ưu đãi, thanh toán, nhả ghế quá hạn), tạo mã QR
    web/                    Các trang (controller), chặn trang cần đăng nhập và trang quản trị
src/main/resources/
    application.properties  Cấu hình: cổng web, kết nối MySQL
    db/                     Bản sao của schema.sql và du-lieu-mau.sql nằm trong web, dùng khi web tự tạo cơ sở dữ liệu trống
    templates/              Các trang HTML (Thymeleaf); templates/quan-tri là phần quản trị
    static/                 CSS, JavaScript, phông chữ, logo, ảnh dự phòng
```

## Chạy trên máy

Cần có JDK 17 trở lên và MySQL (hoặc MariaDB, ví dụ bản đi kèm XAMPP).

1. **Bật MySQL.** Với XAMPP: mở XAMPP Control Panel, bấm Start ở dòng MySQL. Không cần tự tạo cơ sở dữ liệu: lần chạy đầu tiên web tự tạo cơ sở dữ liệu `scarlet_cinema`, 7 bảng và dữ liệu mẫu.
2. **Chạy web.**
   - Windows: bấm đúp `chay-web.bat`. File này tự tìm Java; máy chưa có Maven thì nó tải Maven 3.9.9 vào thư mục `.maven`.
   - Hoặc, nếu máy đã có Maven: `mvn spring-boot:run`.
3. Thấy dòng `Started ScarletCinemaApplication` thì mở trình duyệt vào <http://localhost:8080>.

Web chỉ tự tạo bảng khi cơ sở dữ liệu chưa có bảng nào của rạp; đã có dữ liệu thì không bao giờ xóa hay nạp đè. Muốn tự tay tạo thì chạy `database/schema.sql` rồi `database/du-lieu-mau.sql` (bằng phpMyAdmin, thẻ Nhập, hoặc `mysql -u root -p < database/schema.sql`).

Lịch chiếu trong dữ liệu mẫu được tạo theo ngày nạp: 7 ngày tính từ hôm đó. Muốn làm mới lịch chiếu thì chạy lại `du-lieu-mau.sql` (file này xóa dữ liệu cũ rồi nạp lại).

## Cấu hình

Mặc định web nối vào MySQL trên máy (`localhost:3306`, cơ sở dữ liệu `scarlet_cinema`, tài khoản `root`, không mật khẩu). Muốn đổi thì đặt biến môi trường trước khi chạy, không cần sửa mã nguồn.

| Biến | Ý nghĩa | Mặc định |
| --- | --- | --- |
| `PORT` | Cổng chạy web | `8080` (trong `Dockerfile.vercel` đặt là `80`) |
| `DB_URL` | Địa chỉ JDBC của MySQL | `jdbc:mysql://localhost:3306/scarlet_cinema` kèm các tham số trong `application.properties` |
| `DB_USER` | Tài khoản MySQL | `root` |
| `DB_PASSWORD` | Mật khẩu MySQL | để trống |
| `ADMIN_PASSWORD` | Không bắt buộc. Đặt (từ 8 ký tự) thì lúc khởi động web dùng nó làm mật khẩu tài khoản quản trị | không đặt: giữ mật khẩu đang có |

## Đưa lên mạng (Vercel + MySQL trên mạng)

Web chạy được trên Vercel dưới dạng container, dùng một MySQL trên mạng (ví dụ Aiven).

1. Tạo một dịch vụ MySQL trên mạng. Không cần tạo bảng hay nạp dữ liệu: web tự làm ở lần khởi động đầu tiên, trong cơ sở dữ liệu mà `DB_URL` trỏ tới.
2. Trên Vercel, tạo dự án từ repo này. Hai file `vercel.json` và `Dockerfile.vercel` ở thư mục gốc cho Vercel biết cách build và chạy web; không cần chỉnh gì thêm ở phần Build.
3. Trong phần Environment Variables của dự án, đặt `DB_URL`, `DB_USER`, `DB_PASSWORD`, và nên đặt thêm `ADMIN_PASSWORD` để tài khoản quản trị không dùng mật khẩu mẫu (bảng ở mục Cấu hình). Với MySQL bắt buộc kết nối mã hóa, thêm `sslMode=REQUIRED` vào `DB_URL`, ví dụ:
   `jdbc:mysql://MÁY-CHỦ:CỔNG/scarlet_cinema?createDatabaseIfNotExist=true&sslMode=REQUIRED&useUnicode=true&characterEncoding=UTF-8&sessionVariables=sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION'`
   (phần `sessionVariables=sql_mode=...` cần cho Aiven, vì Aiven mặc định bật chế độ ANSI làm một số câu lệnh của web bị hiểu sai).
4. Không ghi mật khẩu vào mã nguồn.

Khi không có ai truy cập một lúc, máy chủ tự tắt để tiết kiệm; lần truy cập đầu tiên sau đó sẽ chậm hơn vài giây vì web phải khởi động lại.

## Tài khoản thử

Dữ liệu mẫu có một quản trị viên, mười khách và sáu mã ưu đãi (bốn mã đang dùng được, một mã hết hạn, một mã hết lượt). Email và mật khẩu ghi ở phần đầu file `database/du-lieu-mau.sql`. Các tài khoản này chỉ để chạy thử; hãy đổi mật khẩu trước khi dùng thật.

## Các địa chỉ chính

| Địa chỉ | Trang |
| --- | --- |
| `/` | Trang chủ |
| `/phim/{mã phim}` | Chi tiết phim và lịch chiếu |
| `/phim-sap-chieu` | Toàn bộ phim sắp chiếu |
| `/dat-ve/{mã suất}` | Chọn ghế (cần đăng nhập) |
| `/thanh-toan/{mã đơn}` | Thanh toán đơn đang giữ ghế, nhập mã ưu đãi |
| `/ve/{mã đơn}` | Vé có mã QR (chỉ có sau khi thanh toán) |
| `/ve-cua-toi` | Lịch sử vé |
| `/thong-tin` | Giới thiệu, quy định đặt vé, hỏi đáp, bảo mật |
| `/dang-nhap`, `/dang-ky` | Đăng nhập, đăng ký |
| `/quan-tri/phim`, `/quan-tri/suat-chieu`, `/quan-tri/ve` | Quản trị: phim, suất chiếu, vé đã đặt và mã ưu đãi (chỉ quản trị viên) |

## Cơ sở dữ liệu

Bảy bảng: `users`, `movies`, `rooms`, `showtimes`, `promotions`, `bookings`, `tickets`. Sơ đồ quan hệ ở `database/so-do-bang.png`.

- Ràng buộc `UNIQUE (showtime_id, seat_code)` trên bảng `tickets` bảo đảm mỗi ghế của một suất chiếu chỉ bán một lần, kể cả khi hai người bấm đặt cùng lúc.
- Đơn đặt vé có hai trạng thái: `PENDING` (đang giữ ghế, chờ thanh toán) và `PAID` (đã thanh toán). Đơn quá hạn chưa thanh toán bị xóa, vé của đơn bị xóa theo nên ghế bán lại được.
- Bảng `promotions` lưu mã ưu đãi. Đơn có dùng mã ghi `promotion_id` và `discount_amount`; `total_amount` là số tiền phải trả, đã trừ phần giảm. Lúc áp mã, web khóa dòng của mã đó (`SELECT ... FOR UPDATE`) để hai người không cùng dùng lượt cuối. Đơn bị hủy hoặc quá hạn thì lượt dùng mã được trả lại.
- `database/truy-van-mau.sql` có 17 câu truy vấn mà web dùng; `database/kiem-tra-rang-buoc.sql` có 13 lệnh cố tình làm sai để thấy cơ sở dữ liệu chặn lại.

## Ghi chú

- Web đã chạy với MariaDB 10.4 (bản của XAMPP); các file SQL chạy được trên MySQL 8.
- Poster phim được tải qua đường dẫn bên ngoài. Khi không tải được, web dùng ảnh dự phòng trong `static/posters`.
- Trailer phát bằng trình phát nhúng của YouTube, đường dẫn lưu ở cột `movies.trailer_url`. Phim chưa có đường dẫn thì nút "Xem trailer" mở kết quả tìm trailer trên YouTube.
- Cơ sở dữ liệu tạo từ bản trước chưa có cột `trailer_url`: web tự thêm cột này và điền trailer cho các phim mẫu ở lần khởi động đầu tiên (lớp `DatabaseUpgrade`), không làm mất dữ liệu đang có.
- Mã ưu đãi được thêm, sửa trực tiếp trong bảng `promotions`; web chưa có trang quản trị để nhập mã.
- Phiên đăng nhập lưu trong MySQL (hai bảng `SPRING_SESSION` và `SPRING_SESSION_ATTRIBUTES`, web tự tạo lúc khởi động), nên web khởi động lại người dùng vẫn còn đăng nhập.
- Phông chữ Anton, Be Vietnam Pro và JetBrains Mono dùng giấy phép SIL Open Font License. Bootstrap dùng giấy phép MIT, ZXing dùng giấy phép Apache 2.0.
