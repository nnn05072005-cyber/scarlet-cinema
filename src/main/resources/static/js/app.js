// Nút "Xóa" ở trang quản trị: bấm lần đầu thì nút đổi thành "Bấm lần nữa để xóa",
// bấm lần hai trong 4 giây mới thật sự gửi lệnh xóa. Tránh xóa nhầm.
document.querySelectorAll("form.can-xac-nhan").forEach(function (mau) {
  var nut = mau.querySelector("button");
  var chuCu = nut.textContent;
  var daHoi = false;
  mau.addEventListener("submit", function (e) {
    if (daHoi) { return; }
    e.preventDefault();
    daHoi = true;
    nut.textContent = "Bấm lần nữa để xóa";
    setTimeout(function () { daHoi = false; nut.textContent = chuCu; }, 4000);
  });
});
