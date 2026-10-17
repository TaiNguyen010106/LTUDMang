package service;

import dao.StudentDAO;
import model.Student;

import java.sql.SQLException;
import java.util.List;

public class StudentService {
    private final StudentDAO dao = new StudentDAO();

    public List<Student> getAllStudents() throws SQLException { return dao.getAllStudents(); }
    public boolean exists(String maSv) throws SQLException { return dao.exists(maSv); }
    public void addStudent(Student s) throws SQLException { dao.addStudent(s); }
    public void updateStudent(Student s) throws SQLException { dao.updateStudent(s); }
    public void deleteStudent(String maSv) throws SQLException { dao.deleteStudent(maSv); }
    public List<Student> searchStudents(String keyword) throws SQLException { return dao.searchStudents(keyword); }
    public List<Student> sortByDiemTb(boolean asc) throws SQLException { return dao.sortByDiemTb(asc); }
    public int countStudents() throws SQLException { return dao.countStudents(); }
    public double getAverageDiemTb() throws SQLException { return dao.getAverageDiemTb(); }
}
