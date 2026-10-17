# Kịch bản trình diễn Client-Server

1. Trên Windows Server 2019, xác nhận IIS và Node.js đang chạy; cổng 80 và 5001 ở trạng thái Listen.
2. Trên Windows 11, chạy `start-npt-browser.cmd`.
3. Trong tab **Trình duyệt**, mở `http://NPT-SERVER/`, đăng nhập và thử chat.
4. Trong tab **HTTP Tester**, lần lượt gửi:
   - `GET http://NPT-SERVER:5001/api/time`
   - `POST http://NPT-SERVER:5001/api/message` với body `{"message":"Xin chao NPT"}`
   - `GET http://NPT-SERVER:5001/api/students`
   - `GET http://NPT-SERVER:5001/abc`
5. Chỉ ra status code, response headers, body và thời gian phản hồi hiển thị từ response thật.
6. Dừng Backend để minh họa lỗi kết nối, sau đó khởi động lại đúng một process.
7. Chạy `scripts/Test-Server.ps1` để trình diễn nhiều request gần đồng thời.

`localhost` trên Windows 11 trỏ về máy Client. Vì vậy NPT Browser phải dùng
hostname của Windows Server VMware như `NPT-SERVER`, không dùng `localhost`.


## Demo giao diện 1.3.0: Space Edition, tab dọc và tìm kiếm
1. Mở NPT Browser 1.3.0 và quan sát nền vũ trụ, logo, ô tìm kiếm và các ô truy cập nhanh.
2. Rê chuột vào thanh biểu tượng bên trái; danh sách tab cần bung ra với tên trang, host và nút đóng. Rê chuột ra ngoài để danh sách thu gọn.
3. Nhấp ô `Shrimple` hoặc nhập `NPT-SERVER` để mở IIS; nhập `Java HttpClient` để tìm kiếm web.
4. Nhấn nút `+` hoặc `Ctrl+T`, mở thêm địa chỉ khác rồi chuyển tab từ thanh dọc bên trái.
5. Dùng `Ctrl+W` hoặc nút `×` trong danh sách tab để đóng; kiểm tra `Ctrl+Tab`, Back, Forward và Reload.
6. Truy cập trang có liên kết `target=_blank` để kiểm tra popup mở trong tab mới.
7. Chọn `HTTP TESTER` ở thanh tiêu đề để kiểm thử API, sau đó quay lại `TRÌNH DUYỆT`.
8. Trong thanh địa chỉ, tìm `C++ & Java #1 tiếng Việt %`, xác nhận chuỗi được tìm kiếm đúng; sau khi trang mở, nhấn `Ctrl+L` hoặc nhập lại truy vấn và kiểm tra có thể sửa/thay thế nội dung.
9. Thử URL có ký tự Unicode hoặc dấu cách trong query, ví dụ `https://example.com/search?q=tiếng Việt&n=100%`; các ký tự cần thiết phải được mã hóa hợp lệ thay vì làm hỏng URL.


## Demo 1.3.0 — thanh địa chỉ và lối tắt

1. Mở `start-npt-browser.cmd` và tìm kiếm `youtube.com`.
2. Ngay sau khi trang mở, nhập truy vấn khác trên thanh URL và nhấn Enter, không chuyển tab giữa hai lượt.
3. Quay lại trang chủ, bấm **Thêm lối tắt**, nhập tên và URL; kiểm tra thẻ mới xuất hiện.
4. Bấm `×` trên thẻ muốn xóa và xác nhận. Đóng/mở lại ứng dụng để xác nhận danh sách được lưu.
5. Thử tìm kiếm có `+`, `&`, `#`, `%` và dấu tiếng Việt.
6. Mở Shrimple qua IP Server. Nếu nhận HTTP 500 hoặc API không kết nối được, chuyển sang runbook Server/IIS; đây không phải lỗi mã hóa của thanh địa chỉ.
