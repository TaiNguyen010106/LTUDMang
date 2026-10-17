# TCP Multi-Client Server bằng Java

Project sử dụng Java Socket thuần, không framework, database hay dependency
ngoài. `Server.java` và `Client.java` được mở rộng trực tiếp từ phiên bản
Client-Server một Client ban đầu.

## Kiến trúc

- `ServerSocket` lắng nghe tại port `5000` và chạy vòng lặp `accept()`.
- Mỗi `accept()` tạo một `Socket` riêng cho Client, cấp ID bằng
  `AtomicInteger`, đăng ký vào `ConcurrentHashMap` rồi giao một
  `Runnable` cho `ExecutorService`.
- `handleClient` đọc từng dòng bằng `BufferedReader`. Mỗi dòng được trả về
  cho người gửi dưới dạng `UPPERCASE: ...`, đồng thời broadcast dưới dạng
  `Client #N: ...` cho các Client khác.
- `ClientConnection.send()` được `synchronized` để nhiều task không ghi
  xen kẽ vào cùng một output stream.
- Khi đọc được `exit`, EOF hoặc IOException, handler ghi thời gian ngắt kết
  nối, xóa Client khỏi danh sách, giảm bộ đếm và đóng socket.
- Client có một thread riêng để nhận cả phản hồi uppercase và broadcast trong
  khi thread chính đọc bàn phím.

## Cách biên dịch và chạy

### 1. Kiểm tra Java

Cần cài JDK 8 trở lên. Kiểm tra trong PowerShell hoặc Command Prompt:

```text
java -version
javac -version
```

### 2. Mở terminal tại thư mục project

Ví dụ trên Windows PowerShell:

```powershell
cd D:\code\LTM\JavaSocketProgramming
```

### 3. Biên dịch toàn bộ chương trình

```powershell
javac Server.java Client.java
```

Nếu biên dịch thành công, các file `.class` sẽ được tạo trong thư mục
project.

### 4. Chạy Server

Mở **Terminal 1** và chạy:

```powershell
java Server
```

Kết quả mong đợi:

```text
Server đang chạy tại port 5000...
Đang chờ Client kết nối...
```

Phải chạy Server trước khi chạy Client.

### 5. Chạy nhiều Client

Mở **Terminal 2** tại cùng thư mục và chạy:

```powershell
java Client
```

Tiếp tục mở **Terminal 3** và **Terminal 4**, mỗi terminal chạy:

```powershell
java Client
```

Mỗi Client sẽ được cấp một ID khác nhau:

```text
Bạn là Client #1
Bạn là Client #2
Bạn là Client #3
```

### 6. Gửi tin nhắn và chat

Server cũng có thể chủ động gửi tin nhắn. Tại **Terminal 1**, nơi đang chạy
Server, nhập:

```text
Server xin chào mọi người
```

Tất cả Client đang kết nối sẽ nhận:

```text
Server: Server xin chào mọi người
```

Việc nhập tin nhắn ở Server chạy trên luồng riêng nên Server vẫn tiếp tục
`accept()` Client mới.

Tại một Client, nhập:

```text
hello java socket
```

Client gửi sẽ nhận:

```text
UPPERCASE: HELLO JAVA SOCKET
```

Các Client khác sẽ nhận:

```text
Client #1: hello java socket
```

Để ngắt một Client, nhập:

```text
exit
```

Server vẫn tiếp tục chạy và có thể nhận Client mới.

### 7. Dừng chương trình

- Tại Client: nhập `exit`.
- Tại Server: nhấn `Ctrl+C`.

### 8. Chạy Server và Client trên hai máy

Trên máy chạy Server, khởi động:

```powershell
java Server
```

Trong `Client.java`, đổi:

```java
private static final String SERVER_HOST = "127.0.0.1";
```

thành địa chỉ IP của máy chạy Server, ví dụ:

```java
private static final String SERVER_HOST = "192.168.1.10";
```

Sau đó biên dịch lại `Client.java` trên máy Client:

```powershell
javac Client.java
java Client
```

Hai máy phải kết nối cùng mạng, máy Server phải cho phép kết nối TCP đến
port `5000` trong firewall, và port này không được bị chương trình khác sử
dụng.

## Các bài tập đã thực hiện

1. **ID Client:** ID tăng an toàn bằng `AtomicInteger`, không trùng khi Client
   kết nối gần như đồng thời.
2. **Đếm Client:** `connectedClientCount` được cập nhật nguyên tử khi đăng ký
   và khi `disconnect`; danh sách hiện tại nằm trong `ConcurrentHashMap`.
3. **Thời gian:** Server ghi thời điểm connect/disconnect theo `HH:mm:ss` và
   thời lượng kết nối.
4. **Uppercase:** gửi `hello java socket` sẽ nhận
   `UPPERCASE: HELLO JAVA SOCKET`.
5. **Chat:** các Client khác nhận `Client #N: nội dung`; người gửi không nhận
   lại bản broadcast nhưng vẫn nhận phản hồi uppercase.

## Kịch bản kiểm thử

1. Chạy Server, sau đó chạy ba Client. Kiểm tra Server in `Client #1`, `#2`,
   `#3` và số Client đang kết nối là 3.
2. Từ Client 1 gửi `hello java socket`; Client 1 nhận uppercase, Client 2 và
   3 nhận `Client #1: hello java socket`.
3. Gõ `exit` ở Client 1. Server phải in thời gian disconnect, số Client còn
   2, và Client 2/3 vẫn hoạt động.
4. Chạy Client mới. Client mới nhận ID tiếp theo và Server tiếp tục accept.
5. Đóng nhanh nhiều Client hoặc đóng terminal. Server không dừng và bộ đếm
   trở về đúng số kết nối còn lại.

## Giải thích ngắn gọn

- `ServerSocket` chỉ lắng nghe; `Socket` sau `accept()` mới dùng để trao đổi
  dữ liệu hai chiều.
- `accept()` bị chặn cho đến khi có Client, vì vậy Server phải đặt nó trong
  vòng lặp để nhận Client tiếp theo.
- Nếu xử lý đọc/ghi ngay trong thread chính, một Client có thể làm Server
  không nhận được Client khác. `ExecutorService` chạy mỗi handler trong task
  riêng nên các Client hoạt động đồng thời.
- Danh sách Client cần cho broadcast. Cấu trúc concurrent và cập nhật nguyên
  tử tránh race condition khi nhiều handler connect/disconnect cùng lúc.
- `InputStream`/`OutputStream` là luồng byte của socket; `BufferedReader` và
  `PrintWriter` giúp giao tiếp từng dòng. Mỗi `println()` được flush ngay.
- Đóng socket giải phóng tài nguyên và làm `readLine()` ở phía đối diện trả
  `null` hoặc ném `IOException`, nhờ đó Server phát hiện disconnect.
- Port `5000` là số hiệu dịch vụ; hai máy khác nhau cần IP của máy Server,
  kết nối mạng phù hợp và firewall cho phép port này.
