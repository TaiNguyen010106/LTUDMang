# Báo cáo kiểm thử NPT Browser 1.3.0

Cập nhật: 17/10/2026 (Asia/Saigon).

## Kết quả trong môi trường đóng gói

- Biên dịch toàn bộ `src/main/java` bằng `javac -encoding UTF-8 --release 17`: **thành công**.
- Chạy `ManualTestRunner`: **22 đạt, 0 không đạt**.
- JAR phát hành: cập nhật các class mới đã biên dịch lên nền JAR Shade 1.2.1 có sẵn; manifest vẫn trỏ đúng `vn.edu.nptbrowser.app.NptBrowserApp`. Không thay đổi dependency bên thứ ba.
- Không có Maven cài đặt trong môi trường này, vì vậy **chưa chạy lại** `mvn clean test package` đầy đủ.
- Chưa thể kiểm thử tương tác giao diện/JCEF thực tế trên Windows 11 trong môi trường đóng gói. Trạng thái focus của thanh địa chỉ, hiệu ứng căn chỉnh và dialog thêm/xóa cần người dùng xác nhận trên máy đích.

## 22 kiểm thử logic đã chạy

1. Chuẩn hóa URL không có scheme.
2. Giữ URL HTTPS hợp lệ.
3. Từ chối `file://`.
4. Từ chối URL thiếu host.
5. Nhận diện `localhost` và cổng API.
6. Nhận diện URL có scheme.
7. Tự ưu tiên HTTPS cho tên miền công khai.
8. Chuyển từ khóa sang Google Search.
9. Mã hóa UTF-8 cho `C++ & Java #1 tiếng Việt %`.
10. Mã hóa URL có khoảng trắng và tiếng Việt.
11. Nhận diện URL có path/query Unicode.
12. Escape dấu ngoặc và khoảng trắng trong URL.
13. Chuyển hostname Unicode sang IDN.
14. Chuyển từ đơn thành truy vấn tìm kiếm.
15. Parse header trùng tên.
16. Từ chối header không hợp lệ.
17. Lưu/đọc lại cấu hình ứng dụng.
18. Tạo bốn lối tắt mặc định.
19. Lưu/đọc tên tiếng Việt và URL có ký tự đặc biệt trong lối tắt.
20. Từ chối lối tắt trùng URL.
21. Xóa hết lối tắt và giữ trạng thái rỗng sau khi mở lại store.
22. Gửi HTTP POST bất đồng bộ tới HTTP server local và nhận `200`.

## Kiểm thử chưa xác nhận trên máy Windows 11

- Nhấn Enter sau khi tìm kiếm, nhập truy vấn thứ hai ngay trong thanh địa chỉ mà không chuyển tab.
- Gõ `C++ & Java #1 tiếng Việt %` và xác nhận trang kết quả hiển thị/truy vấn đúng.
- Kiểm tra website Shrimple có khai báo UTF-8 hợp lệ; switch default-encoding của Chromium không thể sửa một server cố ý gửi charset sai trong HTTP header.
- Thêm, xóa một lối tắt, tắt mở NPT Browser và xác nhận danh sách vẫn được lưu trong `config/quick-links.properties`.
- Kiểm thử tải thật qua IIS và API. Tình trạng trước đó cần kiểm tra lại trên Server: IIS trả HTTP 500 và TCP 5001 từ chối kết nối.

## Lịch sử ghi nhận

Bản 1.2.1 có runner logic 18/18. Bản 1.3.0 thêm focus-lock, tuỳ chọn mã hóa mặc định UTF-8 cho JCEF, giao diện căn giữa ô tìm kiếm, quản lý lối tắt có lưu bền và kiểm thử store.
