package model;

import java.time.LocalDate;

public class Student {
    private String maSv;
    private String hoTen;
    private LocalDate ngaySinh;
    private String gioiTinh;
    private String lop;
    private String email;
    private String soDienThoai;
    private double diemTb;

    public Student() {}

    public Student(String maSv, String hoTen, LocalDate ngaySinh, String gioiTinh,
                   String lop, String email, String soDienThoai, double diemTb) {
        this.maSv = maSv;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.lop = lop;
        this.email = email;
        this.soDienThoai = soDienThoai;
        this.diemTb = diemTb;
    }

    public String getMaSv() { return maSv; }
    public void setMaSv(String maSv) { this.maSv = maSv; }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public LocalDate getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }
    public String getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; }
    public String getLop() { return lop; }
    public void setLop(String lop) { this.lop = lop; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public double getDiemTb() { return diemTb; }
    public void setDiemTb(double diemTb) { this.diemTb = diemTb; }

    @Override
    public String toString() {
        return maSv + " - " + hoTen;
    }
}
