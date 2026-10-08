package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.Department;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Database access for departments. SQL only - rules live in DepartmentService. */
public class DepartmentDAO {

    private static final String SELECT =
            "SELECT department_id, department_code, department_name FROM departments ";

    public List<Department> findAll() throws DatabaseException {
        return findMany(SELECT + "ORDER BY department_code");
    }

    public Department findById(int departmentId) throws DatabaseException {
        List<Department> list = findMany(SELECT + "WHERE department_id = ?", departmentId);
        return list.isEmpty() ? null : list.get(0);
    }

    public Department findByCode(String code) throws DatabaseException {
        List<Department> list = findMany(SELECT + "WHERE department_code = ?", code);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Department> search(String keyword) throws DatabaseException {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String like = "%" + keyword.trim() + "%";
        return findMany(SELECT + "WHERE department_code LIKE ? OR department_name LIKE ? ORDER BY department_code",
                like, like);
    }

    public boolean codeExists(String code, int excludeId) throws DatabaseException {
        return count("SELECT COUNT(*) FROM departments WHERE department_code = ? AND department_id <> ?",
                code, excludeId) > 0;
    }

    public boolean nameExists(String name, int excludeId) throws DatabaseException {
        return count("SELECT COUNT(*) FROM departments WHERE department_name = ? AND department_id <> ?",
                name, excludeId) > 0;
    }

    public int countCourses(int departmentId) throws DatabaseException {
        return count("SELECT COUNT(*) FROM courses WHERE department_id = ?", departmentId);
    }

    public int countUsers(int departmentId) throws DatabaseException {
        return count("SELECT COUNT(*) FROM users WHERE department_id = ?", departmentId);
    }

    public int countAll() throws DatabaseException {
        return count("SELECT COUNT(*) FROM departments");
    }

    public int insert(Department department) throws DatabaseException {
        String sql = "INSERT INTO departments (department_code, department_name) VALUES (?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, department.getDepartmentCode());
            ps.setString(2, department.getDepartmentName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    department.setDepartmentId(keys.getInt(1));
                }
            }
            return department.getDepartmentId();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to add department: " + e.getMessage(), e);
        }
    }

    public void update(Department department) throws DatabaseException {
        String sql = "UPDATE departments SET department_code = ?, department_name = ? WHERE department_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, department.getDepartmentCode());
            ps.setString(2, department.getDepartmentName());
            ps.setInt(3, department.getDepartmentId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update department: " + e.getMessage(), e);
        }
    }

    public void delete(int departmentId) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM departments WHERE department_id = ?")) {
            ps.setInt(1, departmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete department: " + e.getMessage(), e);
        }
    }

    // ----- helpers -----

    private List<Department> findMany(String sql, Object... params) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Department> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Department(rs.getInt("department_id"),
                            rs.getString("department_code"), rs.getString("department_name")));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to read departments.", e);
        }
    }

    private int count(String sql, Object... params) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query departments.", e);
        }
    }
}