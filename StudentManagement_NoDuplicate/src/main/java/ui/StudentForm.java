package ui;

import model.Student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class StudentForm extends JPanel {
    public final JTextField txtMaSv = createField();
    public final JTextField txtHoTen = createField();
    public final JTextField txtNgaySinh = createField();
    public final JComboBox<String> cboGioiTinh =
            new JComboBox<>(new String[]{"Nam", "Nữ", "Khác"});
    public final JTextField txtLop = createField();
    public final JTextField txtEmail = createField();
    public final JTextField txtSdt = createField();
    public final JTextField txtDiemTb = createField();

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public StudentForm() {
        setOpaque(true);
        setBackground(UITheme.PANEL);
        setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(18, 20, 18, 20)
        ));
        setLayout(new GridBagLayout());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 7, 6, 7);
        g.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel title = new JLabel("HỒ SƠ SINH VIÊN");
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setFont(UITheme.FONT_SECTION);

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 4;
        g.weightx = 1.0;
        g.anchor = GridBagConstraints.WEST;
        add(title, g);

        g.gridwidth = 1;
        g.weightx = 0;

        addField(g, 0, 1, "Mã sinh viên", txtMaSv);
        addField(g, 2, 1, "Họ tên", txtHoTen);

        addField(g, 0, 2, "Ngày sinh", txtNgaySinh);
        addField(g, 2, 2, "Giới tính", cboGioiTinh);

        addField(g, 0, 3, "Lớp", txtLop);
        addField(g, 2, 3, "Email", txtEmail);

        addField(g, 0, 4, "Số điện thoại", txtSdt);
        addField(g, 2, 4, "Điểm trung bình", txtDiemTb);

        txtNgaySinh.setToolTipText("Định dạng: yyyy-MM-dd");
        txtDiemTb.setToolTipText("Nhập điểm từ 0 đến 10");
    }

    private void addField(GridBagConstraints g, int x, int y,
                           String label, JComponent component) {
        g.gridx = x; g.gridy = y; g.weightx = 0;
        add(makeLabel(label), g);

        g.gridx = x + 1; g.weightx = 1;
        component.setPreferredSize(new Dimension(175, 32));
        add(component, g);
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UITheme.TEXT_PRIMARY);
        label.setFont(UITheme.FONT_BOLD);
        return label;
    }

    private static JTextField createField() {
        JTextField f = new JTextField();
        f.setPreferredSize(new Dimension(175, 32));
        f.setBackground(UITheme.FIELD);
        f.setForeground(UITheme.TEXT_PRIMARY);
        f.setCaretColor(UITheme.ACCENT);
        f.setFont(UITheme.FONT_NORMAL);
        f.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
        return f;
    }

    public Student getStudentFromForm() {
        String ma = txtMaSv.getText().trim();
        String ten = txtHoTen.getText().trim();
        String ngay = txtNgaySinh.getText().trim();
        String lop = txtLop.getText().trim();
        String email = txtEmail.getText().trim();
        String sdt = txtSdt.getText().trim();
        String diemText = txtDiemTb.getText().trim();

        if (ma.isEmpty() || ten.isEmpty() || ngay.isEmpty() || lop.isEmpty()
                || email.isEmpty() || sdt.isEmpty() || diemText.isEmpty())
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin.");

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
            throw new IllegalArgumentException("Email không hợp lệ.");

        if (!sdt.matches("\\d{9,11}"))
            throw new IllegalArgumentException("Số điện thoại phải gồm 9-11 chữ số.");

        double diem;
        try { diem = Double.parseDouble(diemText); }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Điểm trung bình phải là số.");
        }

        if (diem < 0 || diem > 10)
            throw new IllegalArgumentException("Điểm trung bình phải từ 0 đến 10.");

        LocalDate ngaySinh;
        try { ngaySinh = LocalDate.parse(ngay, FORMAT); }
        catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày sinh phải có dạng yyyy-MM-dd.");
        }

        if (ngaySinh.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Ngày sinh không được ở tương lai.");

        return new Student(ma, ten, ngaySinh,
                cboGioiTinh.getSelectedItem().toString(),
                lop, email, sdt, diem);
    }

    public void setStudent(Student s) {
        txtMaSv.setText(s.getMaSv());
        txtHoTen.setText(s.getHoTen());
        txtNgaySinh.setText(s.getNgaySinh().format(FORMAT));
        cboGioiTinh.setSelectedItem(s.getGioiTinh());
        txtLop.setText(s.getLop());
        txtEmail.setText(s.getEmail());
        txtSdt.setText(s.getSoDienThoai());
        txtDiemTb.setText(String.valueOf(s.getDiemTb()));
    }

    public void clear() {
        txtMaSv.setText("");
        txtHoTen.setText("");
        txtNgaySinh.setText("");
        cboGioiTinh.setSelectedIndex(0);
        txtLop.setText("");
        txtEmail.setText("");
        txtSdt.setText("");
        txtDiemTb.setText("");
        txtMaSv.setEnabled(true);
        txtMaSv.requestFocus();
    }
}
