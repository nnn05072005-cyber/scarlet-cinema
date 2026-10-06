package vn.scarlet.cinema.model;

/** Số vé và doanh thu của một phim (trang quản trị). */
public record MovieRevenue(String title, int tickets, long revenue) {
}
