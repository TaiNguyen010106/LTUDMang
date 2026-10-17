# NPT Browser

NPT Browser là ứng dụng desktop Java dùng **JCEF/Chromium** để mở website và Java `HttpClient` để kiểm thử HTTP độc lập. Ứng dụng không phải trang web giả lập trình duyệt và không dùng JavaFX WebView.

## Yêu cầu

- Windows 11 64-bit.
- JDK 17 trở lên (`java -version`).
- Kết nối Internet ở lần chạy đầu để Maven lấy dependency và JCEF tải native Chromium. Các lần sau dùng thư mục `runtime/jcef-bundle` đã lưu.
- Server Shrimple/IIS kết nối được qua LAN.

## Chạy nhanh trên máy thật Windows 11

Nhấp đúp `start-npt-browser.cmd`, hoặc mở PowerShell tại thư mục này:

```powershell
.\scripts\Run-NPTBrowser.ps1
```

Lần đầu có thể mất vài phút vì JCEF native khoảng hơn 100 MB. Không đóng cửa sổ khi màn hình khởi động đang hiển thị tiến độ.

## Build và test

Project kèm Maven Wrapper, không cần cài Maven toàn hệ thống:

```powershell
.\mvnw.cmd clean test package
$configArgument = "-Dnpt.config=config/application.properties"
java -Dfile.encoding=UTF-8 $configArgument -jar target\npt-browser-1.3.0-all.jar
```

JAR Maven khi build đầy đủ: `target\npt-browser-1.3.0-all.jar`. ZIP bàn giao kèm JAR chạy sẵn tại `release\npt-browser-1.3.0-all.jar`, nên có thể mở bằng `start-npt-browser.cmd` mà không cần build lại. Ghi chú giới hạn build/kiểm thử của bản bàn giao nằm trong `docs/TEST-REPORT.md`.

## Cấu hình

Sửa `config/application.properties` hoặc nhấn **Cấu hình** trong ứng dụng:

```properties
server.host=NPT-SERVER
home.url=http://${server.host}/
api.baseUrl=http://${server.host}:5001
http.timeoutSeconds=10
```

Đổi `server.host` trong `config/application.properties` thành hostname của máy
Windows Server VMware (ví dụ `NPT-SERVER`). Client sẽ dùng hostname này thay vì
IP DHCP thay đổi. IIS phục vụ frontend ở cổng 80; backend API vẫn chạy riêng ở
cổng 5001.

Không có IP hard-code trong logic Java. Nếu hostname không phân giải được trên
LAN, thêm bản ghi DNS/hosts cho hostname của VMware Server.

## HTTP Tester

Tab HTTP Tester hỗ trợ GET, POST, PUT, PATCH, DELETE; request headers/body; status code; response headers/body; thời gian phản hồi; timeout và lỗi mạng. Mỗi header nhập trên một dòng theo dạng `Tên: Giá trị`.

## Cấu trúc

```text
NPTBrowser/
  config/                 Cấu hình runtime
  docs/                   Hướng dẫn Server, demo và báo cáo test
  scripts/                Script build, chạy, kiểm thử Server
  server-addon/           Express router độc lập cho API bài thực hành
  src/main/java/          Source Java
  src/test/java/          Unit/integration test local
  pom.xml                 Maven build
```

Xem [docs/SERVER-GUIDE.md](docs/SERVER-GUIDE.md) trước khi sửa Server. Trạng thái test thực tế nằm trong [docs/TEST-REPORT.md](docs/TEST-REPORT.md).

## Giao diện 1.2 — Space Edition

- Giao diện **SPACE Edition**: nền vũ trụ, logo gradient NPT, hiệu ứng kính tối và viền neon xanh–tím.
- Thanh tab dọc ở cạnh trái; rê chuột để bung danh sách có tiêu đề, địa chỉ và nút đóng tab.
- Trang chủ có ô tìm kiếm lớn và lối tắt Shrimple, GitHub, Java Docs, YouTube.
- Thanh công cụ dạng pill: Back, Forward, Reload, thanh URL/tìm kiếm, dấu trang và Cài đặt.
- Khung cửa sổ có nút thu nhỏ/phóng to/đóng riêng, đồng thời giữ vùng kéo và thay đổi kích thước.
- Mỗi tab sử dụng một Chromium browser instance riêng; có thể thêm/đóng/chuyển tab.
- Liên kết mở cửa sổ mới (`target="_blank"`) được chuyển thành tab trong cửa sổ NPT Browser.
- Thanh địa chỉ hỗ trợ nhập URL hoặc từ khóa tìm kiếm Google (tự thử Bing một lần nếu Google không tải được). Ví dụ:
  - `http://NPT-SERVER/` — mở website IIS.
  - `NPT-SERVER:5001/api/time` — thêm `http://` và mở API.
  - `localhost:5173` — mở dịch vụ trên máy đang chạy NPT Browser.
  - `tài liệu Java Swing` — tìm kiếm trên Google.
- Phím tắt: `Ctrl+T` tab mới, `Ctrl+W` đóng tab, `Ctrl+L` chọn thanh địa chỉ, `Ctrl+R` tải lại, `Ctrl+Tab` chuyển tab. HTTP Tester mở qua nút riêng ở thanh tiêu đề và vẫn giữ các chức năng request/response cũ.
- HTTP Tester vẫn nằm ở tab cấp cao riêng, không bị loại bỏ.

Lưu ý: nếu chạy từ máy Windows 11, dùng hostname của Windows Server VMware,
không dùng `localhost` vì `localhost` trỏ về máy client.


## Lưu ý bản giao diện 1.3.0

- Trang tab mới sử dụng hình nền vũ trụ `src/main/resources/space-background.jpg`; đây là tài nguyên tĩnh đi kèm JAR, không cần tải ảnh khi chạy.
- Dấu trang hiện lưu tạm trong bộ nhớ của phiên chạy; chưa đồng bộ hoặc lưu lâu dài giữa các lần mở ứng dụng.
- Cần kiểm tra tương tác thực tế trên Windows 11 (rê chuột bung tab, thay đổi kích thước khung, phím tắt và mở liên kết popup). Bộ test logic không thay thế kiểm thử GUI/JCEF trên máy Windows.


## Bản vá 1.2.1 — tìm kiếm và ký tự đặc biệt

- Giữ thanh địa chỉ sẵn sàng chỉnh sửa và tự chọn toàn bộ địa chỉ sau khi nhấn Enter để lần nhập tiếp theo thay thế nội dung cũ.
- Nội dung được đưa vào bộ phân giải URL đúng một lần, tránh mã hóa/tìm kiếm lặp.
- Từ khóa tìm kiếm được mã hóa UTF-8, gồm tiếng Việt, emoji và các ký tự `+`, `&`, `#`, `%`.
- URL có dấu cách, dấu tiếng Việt, ngoặc hoặc dấu phần trăm chưa escape trong path/query được chuẩn hóa an toàn; tên miền quốc tế được chuyển sang IDN.
- Tên miền công khai không kèm scheme ưu tiên HTTPS; IP, localhost, các tên miền nội bộ và địa chỉ có cổng riêng vẫn dùng HTTP.
- Nếu Google Search báo lỗi tải trang trong Chromium, NPT Browser tự thử lại cùng truy vấn bằng Bing một lần.
- Trình khởi chạy chỉ định UTF-8 cho JVM để giảm lỗi charset mặc định trên Windows.


## Bản 1.3.0 — sửa focus, UTF-8 và quản lý lối tắt

- Sau khi gửi tìm kiếm/URL — kể cả từ ô tìm kiếm trang chủ và lối tắt — thanh địa chỉ lấy lại focus sau callback focus của Chromium; focus-lock được bỏ khi người dùng bấm vào vùng trang web hoặc chuyển tab.
- `Ctrl+L` tiếp tục chọn toàn bộ thanh địa chỉ; khi focus vào thanh URL, có thể gõ truy vấn mới mà không cần chuyển tab.
- Ô tìm kiếm trang chủ căn giữa nội dung và placeholder theo chiều ngang; biểu tượng, tiêu đề và địa chỉ trong thẻ lối tắt được căn chỉnh thống nhất.
- JCEF nhận `--lang=vi`, `--accept-lang=...` và `--default-encoding=UTF-8` để ưu tiên hiển thị tiếng Việt nếu website không khai báo charset riêng. Website gửi charset sai trong HTTP header vẫn cần sửa ở server.
- Có nút `+ Thêm lối tắt` và nút `×` trên từng thẻ. Lối tắt được lưu tại `config/quick-links.properties`, dùng chung giữa các tab và còn sau khi khởi động lại.
- Mỗi lối tắt cần tên và URL hợp lệ; URL sẽ được chuẩn hóa trước khi lưu. Khi xóa, ứng dụng hỏi xác nhận.

## Mô hình triển khai IIS + Client

NPT Browser là ứng dụng desktop Swing/JCEF chạy trên laptop client; **IIS không chạy giao diện Swing như một ứng dụng web**. Server Windows Server 2019 nên cung cấp IIS Website (frontend Shrimple), Node.js backend/API/Socket.IO như tiến trình dịch vụ riêng, MongoDB hiện có và IIS FTP cho bài thực hành. Xem `docs/ARCHITECTURE-IIS-CLIENT.md` trước khi thay đổi cấu hình.
