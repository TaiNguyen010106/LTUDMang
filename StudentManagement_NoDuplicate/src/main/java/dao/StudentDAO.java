package dao;

import database.DatabaseConnection;
import model.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {

    public List<Student> getAllStudents() throws SQLException {
        String sql = "SELECT * FROM students ORDER BY ma_sv";
        List<Student> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapStudent(rs));
        }
        return list;
    }

    public boolean exists(String maSv) throws SQLException {
        String sql = "SELECT 1 FROM students WHERE ma_sv = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSv);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void addStudent(Student s) throws SQLException {
        String sql = """
                INSERT INTO students
                (ma_sv, ho_ten, ngay_sinh, gioi_tinh, lop, email, so_dien_thoai, diem_tb)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, s);
            ps.executeUpdate();
        }
    }

    public void updateStudent(Student s) throws SQLException {
        String sql = """
                UPDATE students SET ho_ten=?, ngay_sinh=?, gioi_tinh=?,
                lop=?, email=?, so_dien_thoai=?, diem_tb=? WHERE ma_sv=?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getHoTen());
            ps.setDate(2, Date.valueOf(s.getNgaySinh()));
            ps.setString(3, s.getGioiTinh());
            ps.setString(4, s.getLop());
            ps.setString(5, s.getEmail());
            ps.setString(6, s.getSoDienThoai());
            ps.setDouble(7, s.getDiemTb());
            ps.setString(8, s.getMaSv());
            ps.executeUpdate();
        }
    }

    public void deleteStudent(String maSv) throws SQLException {
        String sql = "DELETE FROM students WHERE ma_sv=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSv);
            ps.executeUpdate();
        }
    }

    public List<Student> searchStudents(String keyword) throws SQLException {
        String sql = """
                SELECT * FROM students
                WHERE ma_sv LIKE ? OR ho_ten LIKE ?
                ORDER BY ma_sv
                """;
        List<Student> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String k = "%" + keyword + "%";
            ps.setString(1, k);
            ps.setString(2, k);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapStudent(rs));
            }
        }
        return list;
    }

    public List<Student> sortByDiemTb(boolean ascending) throws SQLException {
        String direction = ascending ? "ASC" : "DESC";
        String sql = "SELECT * FROM students ORDER BY diem_tb " + direction + ", ma_sv";
        List<Student> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapStudent(rs));
        }
        return list;
    }

    public int countStudents() throws SQLException {
        String sql = "SELECT COUNT(*) FROM students";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public double getAverageDiemTb() throws SQLException {
        String sql = "SELECT COALESCE(AVG(diem_tb), 0) FROM students";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getDouble(1);
        }
    }

    private Student mapStudent(ResultSet rs) throws SQLException {
        return new Student(
                rs.getString("ma_sv"),
                rs.getString("ho_ten"),
                rs.getDate("ngay_sinh").toLocalDate(),
                rs.getString("gioi_tinh"),
                rs.getString("lop"),
                rs.getString("email"),
                rs.getString("so_dien_thoai"),
                rs.getDouble("diem_tb")
        );
    }

    private void setParams(PreparedStatement ps, Student s) throws SQLException {
        ps.setString(1, s.getMaSv());
        ps.setString(2, s.getHoTen());
        ps.setDate(3, Date.valueOf(s.getNgaySinh()));
        ps.setString(4, s.getGioiTinh());
        ps.setString(5, s.getLop());
        ps.setString(6, s.getEmail());
        ps.setString(7, s.getSoDienThoai());
        ps.setDouble(8, s.getDiemTb());
    }
}
