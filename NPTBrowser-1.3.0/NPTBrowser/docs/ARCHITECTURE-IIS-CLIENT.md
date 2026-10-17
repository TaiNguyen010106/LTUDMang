# Kiến trúc triển khai NPT Browser, Shrimple và bài lab IIS/FTP

## Điều quan trọng cần phân biệt

**NPT Browser là ứng dụng desktop chạy trên máy Client. IIS không thể chạy giao diện Java Swing/JCEF bên trong một website.** Đúng vai trò của trình duyệt là chạy NPT Browser trên laptop, rồi dùng nó truy cập các dịch vụ đang chạy trên Windows Server.

Nếu yêu cầu là mọi thành phần *của Shrimple* phải nằm trên Server còn laptop chỉ truy cập như Client, sơ đồ nên như sau:

```text
Laptop Windows 11 (Client)
  ├─ NPT Browser (Java 17 + JCEF/Chromium)
  ├─ FileZilla Client (bài FTP)
  └─ Trình kiểm thử HTTP
         │
         ├── HTTP :80 ───────────────┐
         ├── API :5001 ──────────────┤
         └── FTP :21 + passive data ┤
                                    ▼
Windows Server 2019 / DC1
  ├─ IIS Web Site :80              │  phục vụ Shrimple frontend/dist
  ├─ Node.js service :5001         │  API + Socket.IO; tiến trình riêng
  ├─ MongoDB                       │  giữ nguyên dữ liệu hiện tại
  └─ IIS FTP :21 + passive range ──┘  thư mục/ủy quyền FTP tách biệt
```

## Triển khai an toàn theo từng thành phần

1. **IIS Website:** chỉ trỏ Physical Path tới bản frontend đã build (thường là `client/dist` đã xuất bản), không trỏ IIS vào thư mục source gốc chứa `node_modules`, `.env`, `.git` hoặc dữ liệu riêng.
2. **Node.js API + Socket.IO:** chạy bằng tiến trình/service riêng trên Windows Server, lắng nghe cổng đã chọn. IIS không tự chạy `npm start` chỉ vì website được tạo. Có thể dùng IIS URL Rewrite + ARR làm reverse proxy nếu đã cài và cấu hình WebSocket đúng; chưa được tự ý thêm rule khi chưa kiểm tra `web.config`, binding và các route đang dùng.
3. **MongoDB:** giữ nguyên connection string, dữ liệu và tài khoản hiện có. Không xóa database, không chạy seed/reset khi chẩn đoán kết nối.
4. **IIS FTP:** cấu hình site FTP riêng, home directory và NTFS ACL riêng; không dùng thẳng thư mục gốc website làm thư mục upload nếu chưa có yêu cầu. Mở TCP 21 và dải Passive FTP đã cấu hình; không mở rộng Firewall ra Public nếu lab chỉ cần mạng LAN.
5. **NPT Browser trên laptop:** đặt `server.host` thành hostname cố định của
   Windows Server VMware (ví dụ `NPT-SERVER`). `home.url` dùng
   `http://NPT-SERVER/`; `api.baseUrl` dùng `http://NPT-SERVER:5001`. Không
   phụ thuộc IP DHCP thay đổi. IIS phục vụ frontend; Node.js API chạy riêng.
6. **FileZilla Client:** kết nối tới IP Server, không phải `localhost`. Tạo các entry riêng cho Anonymous và User theo đúng yêu cầu bài lab; kiểm tra quyền FTP và quyền NTFS vì cả hai lớp đều ảnh hưởng kết quả.

## Kiểm tra trước khi triển khai

Trên Windows Server, dùng PowerShell để xem binding và tiến trình đang lắng nghe; không dừng tiến trình lạ hoặc khởi động thêm bản Node thứ hai khi cổng đã bị chiếm:

```powershell
Import-Module WebAdministration
Get-Website | Select-Object Name, State, PhysicalPath, Bindings
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
  Where-Object { $_.LocalPort -in 21, 80, 5001 } |
  Select-Object LocalAddress, LocalPort, OwningProcess
```

Nếu `http://NPT-SERVER/` còn HTTP 500, cần lấy mã substatus (ví dụ 500.19), xem Event Viewer và log IIS trước khi thay đổi cấu hình. Nếu TCP 5001 bị từ chối, hãy xác nhận backend thực sự listen trước khi sửa Firewall.

**Không chép JAR NPT Browser vào web root với kỳ vọng nó trở thành trình duyệt web.** Nếu giảng viên yêu cầu một *client web* chạy qua trình duyệt thông thường, đó là một frontend web riêng và không tương đương với ứng dụng desktop Java/JCEF hiện tại.
