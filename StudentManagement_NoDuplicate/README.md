# StudentManagement-NoDuplicate

Ứng dụng quản lý sinh viên bằng Java Swing + MySQL + JDBC. Đây là bản sao độc lập
của `StudentManagement`, được chỉnh sửa để không thêm trùng mã sinh viên.

## Yêu cầu
- JDK 25+
- MySQL 8+
- Maven

## 1. Tạo database
Mở MySQL Workbench hoặc MySQL command line và chạy:

resources/database.sql

## 2. Cấu hình MySQL
Mở:

src/main/java/database/DatabaseConnection.java

Sửa:
- USERNAME
- PASSWORD

Mặc định:
USERNAME = root
PASSWORD = ""

Nếu MySQL của bạn có mật khẩu thì điền mật khẩu tương ứng.

## 3. Chạy bằng Maven

mvn clean compile
mvn exec:java -Dexec.mainClass=Main

Hoặc mở project bằng IntelliJ IDEA, reload Maven rồi chạy Main.java.

## 4. Chức năng
- Hiển thị danh sách
- Thêm
- Sửa
- Xóa
- Tìm kiếm mã SV / họ tên
- Sắp xếp điểm TB
- Thống kê số lượng
- Thống kê điểm TB
- Làm mới
- Thoát

## 5. Chống trùng mã sinh viên
- `ma_sv` là khóa chính trong database nên mỗi mã chỉ xuất hiện một lần.
- Khi thêm thủ công, mã đã tồn tại sẽ bị từ chối.
- Khi nhập Excel, mã được chuẩn hóa (xóa khoảng trắng, đổi thành chữ in hoa).
- Các mã trùng nhau trong cùng file Excel hoặc đã có trong database sẽ được bỏ qua.
- Có thể chạy lại `resources/database.sql` mà không tạo thêm bản ghi mẫu trùng mã.

## Mở project đúng trong VS Code

Hãy mở trực tiếp thư mục `StudentManagement-NoDuplicate` hoặc thư mục project đã đổi tên,
không mở thư mục cha chứa project lồng bên trong.

Cấu trúc cần thấy:

```text
StudentManagement_ModernUI/
├── pom.xml
├── resources/
└── src/main/java/
    ├── Main.java
    ├── model/Student.java
    ├── service/StudentService.java
    ├── dao/StudentDAO.java
    ├── database/DatabaseConnection.java
    └── ui/
        ├── MainFrame.java
        ├── StudentForm.java
        └── UITheme.java
```

Sau khi giải nén:

1. Mở VS Code → **File → Open Folder** → chọn thư mục có `pom.xml`.
2. Nhấn `Ctrl + Shift + P` → chọn **Java: Clean Java Language Server Workspace**.
3. Chọn **Restart and delete** nếu VS Code hỏi.
4. Chờ Maven tải các thư viện trong `pom.xml`.
5. Chạy file `src/main/java/Main.java`, không chạy trực tiếp `MainFrame.java`.

Lưu ý: file `MainFrame.java` đã được sửa lỗi xung đột `Color` giữa `java.awt.Color`
và Apache POI. Không dùng lại dòng `import org.apache.poi.ss.usermodel.*;`.

## Bố cục giao diện
- Giao diện theo phong cách sáng, nền kem với điểm nhấn xanh ngọc và cam san hô.
- Khối hồ sơ sinh viên nằm bên trái, bảng điều khiển nằm bên phải.
- Danh sách sinh viên dùng bảng phẳng, có màu xen kẽ để dễ đọc.
- Có đầy đủ các nút: Thêm, Sửa, Xóa, Sắp xếp, Làm mới, Thống kê, Nhập dữ liệu từ Excel và Thoát.
- Nhãn và nút dùng tiếng Việt rõ ràng, không phụ thuộc icon.
