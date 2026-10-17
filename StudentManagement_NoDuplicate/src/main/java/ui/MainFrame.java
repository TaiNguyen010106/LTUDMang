package ui;

import model.Student;
import service.StudentService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import javax.swing.table.JTableHeader;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class MainFrame extends JFrame {
    private final StudentService service = new StudentService();
    private final StudentForm form = new StudentForm();
    private final JTextField txtTimKiem = new JTextField();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel lblTong = new JLabel("Tổng sinh viên: 0");
    private final JLabel lblDiemTB = new JLabel("ĐTB lớp: 0.00");

    public MainFrame() {
        // Initialize table and tableModel first
        tableModel = new DefaultTableModel(
                new Object[]{"STT", "Mã SV", "Họ tên", "Ngày sinh", "Giới tính",
                        "Lớp", "Email", "Số điện thoại", "Điểm TB"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(tableModel);
        
        setTitle("Quản Lý Sinh Viên");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 650));
        setSize(1220, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BACKGROUND);

        // Ép JComboBox dùng màu Dark IT, tránh Windows LookAndFeel tô trắng.
        styleGenderComboBox();

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(UITheme.BACKGROUND);
        root.setBorder(new EmptyBorder(18, 22, 14, 22));
        setContentPane(root);

        root.add(createHeader(), BorderLayout.NORTH);
        root.add(createContent(), BorderLayout.CENTER);
        root.add(createStatusBar(), BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillFormFromSelectedRow();
        });

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { exitApp(); }
        });

        loadAll();
    }
private void styleGenderComboBox() {
        JComboBox<String> combo = form.cboGioiTinh;

        combo.setEditable(false);
        combo.setFont(UITheme.FONT_NORMAL);
        combo.setForeground(UITheme.TEXT_PRIMARY);
        combo.setBackground(new Color(226, 241, 234));
        combo.setOpaque(true);
        combo.setPreferredSize(new Dimension(175, 32));

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                label.setOpaque(true);
                label.setFont(UITheme.FONT_NORMAL);
                label.setBackground(isSelected
                        ? new Color(231, 137, 88)
                        : new Color(226, 241, 234));
                label.setForeground(isSelected ? Color.WHITE : UITheme.TEXT_PRIMARY);
                label.setBorder(new EmptyBorder(4, 8, 4, 8));
                return label;
            }
        });

        combo.setUI(new BasicComboBoxUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(226, 241, 234));
                g2.fillRect(0, 0, c.getWidth(), c.getHeight());
                g2.dispose();
                super.paint(g, c);

                Insets in = combo.getInsets();
                int arrowWidth = 28;
                int x = in.left;
                int y = in.top;
                int w = combo.getWidth() - in.left - in.right - arrowWidth;
                int h = combo.getHeight() - in.top - in.bottom;

                Graphics2D g3 = (Graphics2D) g.create();
                g3.setColor(new Color(226, 241, 234));
                g3.fillRect(x, y, Math.max(0, w), Math.max(0, h));
                g3.dispose();

                Object value = combo.getSelectedItem();
                if (value != null) {
                    JLabel label = new JLabel(value.toString());
                    label.setOpaque(false);
                    label.setFont(UITheme.FONT_NORMAL);
                    label.setForeground(UITheme.TEXT_PRIMARY);
                    label.setBorder(new EmptyBorder(4, 8, 4, 4));

                    Graphics2D g4 = (Graphics2D) g.create();
                    SwingUtilities.paintComponent(
                            g4, label, combo, x, y, Math.max(0, w), Math.max(0, h));
                    g4.dispose();
                }
            }

            @Override
            protected JButton createArrowButton() {
                JButton arrow = new BasicArrowButton(
                        SwingConstants.SOUTH,
                        new Color(226, 241, 234), new Color(226, 241, 234),
                        UITheme.ACCENT, new Color(226, 241, 234));
                arrow.setBackground(new Color(226, 241, 234));
                arrow.setForeground(UITheme.ACCENT);
                arrow.setOpaque(true);
                arrow.setBorder(BorderFactory.createEmptyBorder());
                return arrow;
            }
        });

        combo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.ACCENT, 1, true),
                new EmptyBorder(0, 4, 0, 4)
        ));
    }

    private JPanel createHeader() {
        JPanel p = new JPanel(new BorderLayout(18, 0));
        p.setBackground(UITheme.BACKGROUND);
        p.setBorder(new EmptyBorder(0, 2, 6, 2));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("SGU  /  2026");
        eyebrow.setForeground(UITheme.ACCENT);
        eyebrow.setFont(UITheme.FONT_BOLD);

        JLabel title = new JLabel("Quản Lý Sinh Viên");
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setFont(UITheme.FONT_TITLE);

        JLabel sub = new JLabel("Theo dõi hồ sơ sinh viên, điểm số và thông tin liên lạc");
        sub.setForeground(UITheme.TEXT_SECONDARY);
        sub.setFont(UITheme.FONT_SUBTITLE);

        copy.add(eyebrow);
        copy.add(Box.createVerticalStrut(5));
        copy.add(title);
        copy.add(Box.createVerticalStrut(3));
        copy.add(sub);

        JLabel mark = new JLabel("SD", SwingConstants.CENTER);
        mark.setOpaque(true);
        mark.setBackground(UITheme.ACCENT);
        mark.setForeground(Color.WHITE);
        mark.setFont(new Font("Segoe UI", Font.BOLD, 22));
        mark.setPreferredSize(new Dimension(66, 66));

        p.add(mark, BorderLayout.WEST);
        p.add(copy, BorderLayout.CENTER);
        return p;
    }

    private JPanel createContent() {
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setOpaque(false);
        
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(form, BorderLayout.CENTER);
        top.add(left, BorderLayout.CENTER);
        top.add(createActionPanel(), BorderLayout.EAST);

        
        content.add(top, BorderLayout.NORTH);
        content.add(createTablePanel(), BorderLayout.CENTER);
        return content;
    }

    private JLabel section(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UITheme.TEXT_PRIMARY);
        l.setFont(UITheme.FONT_SECTION);
        l.setBorder(new EmptyBorder(0, 3, 7, 0));
        return l;
    }

    private JPanel createActionPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setPreferredSize(new Dimension(390, 300));
        p.setBackground(UITheme.PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        p.add(section("BẢNG ĐIỀU KHIỂN"), BorderLayout.NORTH);

        JPanel c = new JPanel();
        c.setOpaque(false);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);

        txtTimKiem.setBackground(UITheme.FIELD);
        txtTimKiem.setForeground(UITheme.TEXT_PRIMARY);
        txtTimKiem.setCaretColor(UITheme.ACCENT);
        txtTimKiem.setFont(UITheme.FONT_NORMAL);
        txtTimKiem.setPreferredSize(new Dimension(0, 36));
        txtTimKiem.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(5, 9, 5, 9)
        ));
        txtTimKiem.setToolTipText("Tìm theo mã sinh viên hoặc họ tên");

        JButton search = createButton("Lọc danh sách", UITheme.ACCENT);
        search.setPreferredSize(new Dimension(112, 36));
        searchRow.add(txtTimKiem, BorderLayout.CENTER);
        searchRow.add(search, BorderLayout.EAST);
        c.add(searchRow);
        c.add(Box.createVerticalStrut(12));

        JPanel grid = new JPanel(new GridLayout(4, 2, 9, 9));
        grid.setOpaque(false);

        JButton add = createButton("Thêm", UITheme.ACCENT);
        JButton edit = createButton("Sửa", UITheme.SUCCESS);
        JButton del = createButton("Xóa", UITheme.DANGER);
        JButton sort = createButton("Sắp xếp", UITheme.PURPLE);
        JButton refresh = createButton("Làm mới", UITheme.SLATE);
        JButton stats = createButton("Thống kê", UITheme.SUCCESS);
        JButton importExcel = createButton("Nhập từ Excel", UITheme.PURPLE);
        importExcel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JButton exit = createButton("Thoát", UITheme.DANGER);

        for (JButton button : new JButton[]{add, edit, del, sort, refresh, stats, importExcel, exit}) {
            button.setPreferredSize(new Dimension(160, 42));
            button.setMinimumSize(new Dimension(130, 42));
        }

        grid.add(add);
        grid.add(edit);
        grid.add(del);
        grid.add(sort);
        grid.add(refresh);
        grid.add(stats);
        grid.add(importExcel);
        grid.add(exit);

        c.add(grid);
        p.add(c, BorderLayout.CENTER);

        add.addActionListener(e -> addStudent());
        edit.addActionListener(e -> updateStudent());
        del.addActionListener(e -> deleteStudent());
        search.addActionListener(e -> searchStudents());
        txtTimKiem.addActionListener(e -> searchStudents());
        sort.addActionListener(e -> sortStudents());
        refresh.addActionListener(e -> loadAllAndClear());
        stats.addActionListener(e -> showStatistics());
        importExcel.addActionListener(e -> importFromExcel());
        exit.addActionListener(e -> exitApp());

        return p;
    }

    private JButton createButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(125, 36));
        b.setBorder(BorderFactory.createEmptyBorder(7, 9, 7, 9));

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(UITheme.ACCENT_HOVER);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(color);
            }
        });
        return b;
    }

    private JPanel createTablePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel title = section("DANH SÁCH SINH VIÊN");
        title.setBorder(new EmptyBorder(2, 4, 6, 0));
        p.add(title, BorderLayout.NORTH);

        table.setRowHeight(30);
        table.setFont(UITheme.FONT_NORMAL);
        table.setBackground(UITheme.PANEL);
        table.setForeground(UITheme.TEXT_PRIMARY);
        table.setSelectionBackground(new Color(188, 224, 211));
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(new Color(231, 232, 224));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setShowHorizontalLines(true);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(
                        table, value, selected, focus, row, column);
                cell.setBorder(new EmptyBorder(0, 7, 0, 7));
                cell.setForeground(selected ? UITheme.TEXT_PRIMARY : UITheme.TEXT_PRIMARY);
                cell.setBackground(selected
                        ? new Color(188, 224, 211)
                        : (row % 2 == 0 ? UITheme.PANEL : new Color(250, 249, 245)));
                return cell;
            }
        });

        JTableHeader header = table.getTableHeader();

        header.setOpaque(true);
        header.setBackground(UITheme.PANEL_ALT);
        header.setForeground(UITheme.TEXT_PRIMARY);
        header.setFont(UITheme.FONT_BOLD);
        header.setPreferredSize(new Dimension(0, 34));
        header.setReorderingAllowed(false);

        // Renderer riêng cho từng ô header: Windows LAF không thể ép nền trắng.
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            {
                setOpaque(true);
                setHorizontalAlignment(SwingConstants.CENTER);
            }

            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                label.setOpaque(true);
                label.setBackground(UITheme.PANEL_ALT);
                label.setForeground(UITheme.TEXT_PRIMARY);
                label.setFont(UITheme.FONT_BOLD);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createMatteBorder(
                        0, 0, 1, 1, UITheme.BORDER));

                return label;
            }
        });

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int col : new int[]{0, 1, 3, 4, 5, 8})
            table.getColumnModel().getColumn(col).setCellRenderer(center);

        int[] widths = {45, 75, 160, 95, 70, 80, 190, 110, 75};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UITheme.FIELD);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel createStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 4, 0, 4));
        lblTong.setForeground(UITheme.TEXT_SECONDARY);
        lblTong.setFont(UITheme.FONT_BOLD);
        lblDiemTB.setForeground(UITheme.ACCENT_HOVER);
        lblDiemTB.setFont(UITheme.FONT_BOLD);
        p.add(lblTong, BorderLayout.WEST);
        p.add(lblDiemTB, BorderLayout.EAST);
        return p;
    }

    private void addStudent() {
        try {
            Student s = form.getStudentFromForm();
            s.setMaSv(s.getMaSv().trim().toUpperCase(Locale.ROOT));
            if (service.exists(s.getMaSv())) { showError("Mã sinh viên đã tồn tại."); return; }
            service.addStudent(s);
            showInfo("Thêm sinh viên thành công.");
            loadAll(); form.clear();
        } catch (IllegalArgumentException ex) { showError(ex.getMessage()); }
        catch (SQLException ex) { showDatabaseError(ex); }
    }

    private void updateStudent() {
        if (table.getSelectedRow() < 0) { showError("Vui lòng chọn sinh viên cần sửa."); return; }
        try {
            service.updateStudent(form.getStudentFromForm());
            showInfo("Cập nhật sinh viên thành công.");
            loadAll(); form.clear();
        } catch (IllegalArgumentException ex) { showError(ex.getMessage()); }
        catch (SQLException ex) { showDatabaseError(ex); }
    }

    private void deleteStudent() {
        int row = table.getSelectedRow();
        if (row < 0) { showError("Vui lòng chọn sinh viên cần xóa."); return; }
        int modelRow = table.convertRowIndexToModel(row);
        String maSv = tableModel.getValueAt(modelRow, 1).toString();

        int choice = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa sinh viên " + maSv + "?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                service.deleteStudent(maSv);
                showInfo("Xóa sinh viên thành công.");
                loadAll(); form.clear();
            } catch (SQLException ex) { showDatabaseError(ex); }
        }
    }

    private void searchStudents() {
        String keyword = txtTimKiem.getText().trim();
        if (keyword.isEmpty()) { loadAll(); return; }
        try {
            List<Student> list = service.searchStudents(keyword);
            fillTable(list);
            if (list.isEmpty()) showInfo("Không tìm thấy sinh viên.");
        } catch (SQLException ex) { showDatabaseError(ex); }
    }

    private void sortStudents() {
        String[] options = {"Điểm TB tăng dần", "Điểm TB giảm dần"};
        int choice = JOptionPane.showOptionDialog(this, "Chọn kiểu sắp xếp:",
                "Sắp xếp", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        if (choice == -1) return;
        try { fillTable(service.sortByDiemTb(choice == 0)); }
        catch (SQLException ex) { showDatabaseError(ex); }
    }

    private void showStatistics() {
        try {
            int count = service.countStudents();
            double avg = service.getAverageDiemTb();
            JOptionPane.showMessageDialog(this,
                    "THỐNG KÊ SINH VIÊN\n\n"
                            + "Tổng số sinh viên: " + count + "\n"
                            + String.format("Điểm trung bình của lớp: %.2f", avg),
                    "Thống kê", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) { showDatabaseError(ex); }
    }

    private void loadAll() {
        try { fillTable(service.getAllStudents()); updateStatistics(); }
        catch (SQLException ex) { showDatabaseError(ex); }
    }

    private void loadAllAndClear() {
        txtTimKiem.setText("");
        form.clear();
        loadAll();
    }

    private void fillTable(List<Student> list) {
        tableModel.setRowCount(0);
        int stt = 1;
        for (Student s : list)
            tableModel.addRow(new Object[]{stt++, s.getMaSv(), s.getHoTen(),
                    s.getNgaySinh(), s.getGioiTinh(), s.getLop(), s.getEmail(),
                    s.getSoDienThoai(), String.format("%.2f", s.getDiemTb())});
    }

    private void fillFormFromSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int modelRow = table.convertRowIndexToModel(row);
        try {
            String ma = tableModel.getValueAt(modelRow, 1).toString();
            List<Student> result = service.searchStudents(ma);
            if (!result.isEmpty() && result.get(0).getMaSv().equals(ma)) {
                form.setStudent(result.get(0));
                form.txtMaSv.setEnabled(false);
            }
        } catch (SQLException ignored) {}
    }

    private void updateStatistics() {
        try {
            lblTong.setText("Tổng sinh viên: " + service.countStudents());
            lblDiemTB.setText(String.format("ĐTB lớp: %.2f", service.getAverageDiemTb()));
        } catch (SQLException ex) {
            lblTong.setText("Tổng sinh viên: --");
            lblDiemTB.setText("ĐTB lớp: --");
        }
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi",
                JOptionPane.ERROR_MESSAGE);
    }

    private void showDatabaseError(SQLException ex) {
        showError("Không thể thao tác với cơ sở dữ liệu.\n"
                + "Hãy kiểm tra MySQL, database và tài khoản kết nối.\n\n"
                + "Chi tiết: " + ex.getMessage());
    }

    private void importFromExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn file Excel danh sách sinh viên");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "File Excel (*.xlsx)", "xlsx"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = chooser.getSelectedFile();
        int added = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        Set<String> importedCodes = new HashSet<>();

        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            if (sheet.getLastRowNum() < 1) {
                showError("File Excel không có dữ liệu.\nDòng đầu tiên phải là tiêu đề cột.");
                return;
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row, formatter)) continue;

                int excelRow = i + 1;
                try {
                    Student s = readStudentFromExcel(row, formatter);

                    if (s.getMaSv().isBlank() || s.getHoTen().isBlank()) {
                        throw new IllegalArgumentException("Mã SV và Họ tên không được để trống");
                    }
                    s.setMaSv(s.getMaSv().trim().toUpperCase(Locale.ROOT));
                    if (s.getDiemTb() < 0 || s.getDiemTb() > 10) {
                        throw new IllegalArgumentException("Điểm TB phải từ 0 đến 10");
                    }

                    String studentCode = s.getMaSv();
                    if (!importedCodes.add(studentCode) || service.exists(studentCode)) {
                        skipped++;
                        continue;
                    }

                    service.addStudent(s);
                    added++;
                } catch (Exception ex) {
                    errors.add("Dòng " + excelRow + ": " + ex.getMessage());
                }
            }

            loadAll();

            StringBuilder message = new StringBuilder();
            message.append("NHẬP EXCEL HOÀN TẤT\n\n")
                    .append("Đã thêm: ").append(added).append(" sinh viên\n")
                    .append("Bỏ qua trùng mã: ").append(skipped).append(" sinh viên\n")
                    .append("Lỗi: ").append(errors.size()).append(" dòng");

            if (!errors.isEmpty()) {
                message.append("\n\nChi tiết lỗi:\n");
                int max = Math.min(errors.size(), 10);
                for (int i = 0; i < max; i++) {
                    message.append("- ").append(errors.get(i)).append("\n");
                }
                if (errors.size() > max) {
                    message.append("... và ").append(errors.size() - max).append(" lỗi khác.");
                }
            }

            JOptionPane.showMessageDialog(this, message.toString(),
                    "Nhập Excel", errors.isEmpty()
                            ? JOptionPane.INFORMATION_MESSAGE
                            : JOptionPane.WARNING_MESSAGE);

        } catch (Exception ex) {
            showError("Không thể đọc file Excel.\n\n" + ex.getMessage());
        }
    }

    private Student readStudentFromExcel(Row row, DataFormatter formatter) {
        String maSv = getCellText(row, 0, formatter);
        String hoTen = getCellText(row, 1, formatter);
        LocalDate ngaySinh = getCellDate(row.getCell(2), formatter);
        String gioiTinh = getCellText(row, 3, formatter);
        String lop = getCellText(row, 4, formatter);
        String email = getCellText(row, 5, formatter);
        String soDienThoai = getCellText(row, 6, formatter);
        String diemText = getCellText(row, 7, formatter).replace(',', '.');

        if (diemText.isBlank()) {
            throw new IllegalArgumentException("Điểm TB không được để trống");
        }

        double diemTb;
        try {
            diemTb = Double.parseDouble(diemText);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Điểm TB không hợp lệ: " + diemText);
        }

        return new Student(maSv, hoTen, ngaySinh, gioiTinh, lop,
                email, soDienThoai, diemTb);
    }

    private String getCellText(Row row, int column, DataFormatter formatter) {
        Cell cell = row.getCell(column);
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private LocalDate getCellDate(Cell cell, DataFormatter formatter) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;

        if (DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        String text = formatter.formatCellValue(cell).trim();
        if (text.isBlank()) return null;

        String[] patterns = {"dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd", "dd-MM-yyyy"};
        for (String pattern : patterns) {
            try {
                return java.time.LocalDate.parse(text,
                        java.time.format.DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) { }
        }
        throw new IllegalArgumentException("Ngày sinh không hợp lệ: " + text);
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        for (int i = 0; i < 8; i++) {
            if (!getCellText(row, i, formatter).isBlank()) return false;
        }
        return true;
    }

    private void exitApp() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn thoát chương trình?",
                "Xác nhận thoát", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) System.exit(0);
    }
}
