CREATE DATABASE IF NOT EXISTS student_management
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE student_management;

CREATE TABLE IF NOT EXISTS students (
    ma_sv VARCHAR(20) PRIMARY KEY,
    ho_ten VARCHAR(100) NOT NULL,
    ngay_sinh DATE NOT NULL,
    gioi_tinh VARCHAR(10),
    lop VARCHAR(50),
    email VARCHAR(100),
    so_dien_thoai VARCHAR(20),
    diem_tb DECIMAL(4,2) NOT NULL
);

INSERT IGNORE INTO students
(ma_sv, ho_ten, ngay_sinh, gioi_tinh, lop, email, so_dien_thoai, diem_tb)
VALUES
('SV001', 'Nguyễn Văn An', '2005-01-10', 'Nam', 'CNTT01', 'an@gmail.com', '0900000001', 8.50),
('SV002', 'Trần Thị Bình', '2005-03-15', 'Nữ', 'CNTT01', 'binh@gmail.com', '0900000002', 7.80),
('SV003', 'Lê Văn Cường', '2005-06-20', 'Nam', 'CNTT02', 'cuong@gmail.com', '0900000003', 9.00),
('SV004', 'Phạm Thị Dung', '2005-08-12', 'Nữ', 'CNTT02', 'dung@gmail.com', '0900000004', 6.75),
('SV005', 'Hoàng Văn Em', '2005-11-05', 'Nam', 'CNTT01', 'em@gmail.com', '0900000005', 8.10);
