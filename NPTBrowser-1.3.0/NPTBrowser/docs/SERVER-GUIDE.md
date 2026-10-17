# Hướng dẫn Server Windows Server 2019

Trước khi triển khai, đọc [ARCHITECTURE-IIS-CLIENT.md](ARCHITECTURE-IIS-CLIENT.md) để phân biệt NPT Browser desktop client, IIS Website, Node.js API và IIS FTP. Không host JAR Swing/JCEF như một website IIS.

## Trạng thái đã xác minh ngày 10/10/2026

- Máy thật Windows 11 có IP Wi-Fi `192.168.1.8/24`.
- `192.168.1.2` trả lời với hostname `DC1`, ping thành công và TCP 80 mở. Đây là máy Server, không phải máy thật.
- HTTP tới `http://192.168.1.2/` trả `500 Internal Server Error`.
- TCP 5001 bị từ chối, nghĩa là Backend chưa lắng nghe trên địa chỉ/cổng này hoặc Firewall đang chặn.
- Chưa có ZIP/source Shrimple trong tệp đính kèm nên chưa thể kiểm tra route, `.env`, CORS hay Socket.IO.

## Các bước trên máy ảo Windows Server 2019

Mở PowerShell và chạy các lệnh chỉ đọc sau:

```powershell
ipconfig
Get-NetTCPConnection -LocalPort 80,5001 -ErrorAction SilentlyContinue |
    Select-Object LocalAddress,LocalPort,State,OwningProcess
Get-Process -Id (Get-NetTCPConnection -LocalPort 5001 -ErrorAction SilentlyContinue).OwningProcess
```

Kiểm tra IIS:

```powershell
Import-Module WebAdministration
Get-Website | Select-Object Name,State,PhysicalPath,Bindings
Get-WebAppPoolState -Name "DefaultAppPool"
```

Không sửa `web.config` trước khi xem lỗi chi tiết trong Event Viewer và IIS log tại
`C:\inetpub\logs\LogFiles`. Mã 500 hiện tại cần substatus (ví dụ 500.19) mới xác định được nguyên nhân.

Trong thư mục Backend thực tế, chạy:

```powershell
npm start
```

Nếu gặp `EADDRINUSE`, không khởi động thêm bản thứ hai. Xác định process:

```powershell
Get-NetTCPConnection -LocalPort 5001 | Select-Object State,OwningProcess
Get-Process -Id (Get-NetTCPConnection -LocalPort 5001).OwningProcess
```

## Gắn route bài thực hành (chỉ sau khi kiểm tra route hiện có)

Tệp `server-addon/nptLabRoutes.js` là module độc lập, không ghi dữ liệu MongoDB. Không mount nếu Shrimple đã có route cùng tên.

Trong điểm khởi chạy Express, sau `express.json()` và trước middleware 404:

```javascript
const { router: nptLabRoutes, malformedJsonHandler } = require("./duong-dan/nptLabRoutes");
app.use("/api", nptLabRoutes);
app.use(malformedJsonHandler);
```

Giữ middleware 404 ở cuối để `GET /abc` trả 404. CORS phải giới hạn theo origin thực tế; không dùng `origin: "*"` khi có cookie/credentials.

## Firewall

Chỉ khi cổng đang lắng nghe nhưng máy Client không kết nối được, tạo rule cho mạng Private/LAN:

```powershell
New-NetFirewallRule -DisplayName "NPT IIS HTTP" -Direction Inbound -Protocol TCP -LocalPort 80 -Action Allow -Profile Private
New-NetFirewallRule -DisplayName "NPT Node API" -Direction Inbound -Protocol TCP -LocalPort 5001 -Action Allow -Profile Private
```

Không cần tạo rule nếu kết nối đã hoạt động. Không mở các cổng này cho Public profile.
