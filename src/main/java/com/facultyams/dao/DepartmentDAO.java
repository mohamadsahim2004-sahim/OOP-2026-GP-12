package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.Department;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Database access for departments. SQL only - rules live in DepartmentService. */
public class DepartmentDAO {

    private static final String SELECT =
            "SELECT department_id, department_code, department_name FROM departments ";

    private static final String DUPLICATE_MESSAGE = "Another department already has this code or name.";

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
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DatabaseException(DUPLICATE_MESSAGE, e);
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
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DatabaseException(DUPLICATE_MESSAGE, e);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update department: " + e.getMessage(), e);
        }
    }

    /**
     * Updates code, name and head of department in ONE transaction: either all of it is saved or none.
     * Needs database/migrations/002_department_head.sql (departments.head_user_id).
     *
     * @param headUserId the lecturer who heads the department, or null for no head
     */
    public void updateWithHead(Department department, Integer headUserId) throws DatabaseException {
        String sql = "UPDATE departments SET department_code = ?, department_name = ?, head_user_id = ? "
                + "WHERE department_id = ?";
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, department.getDepartmentCode());
                ps.setString(2, department.getDepartmentName());
                if (headUserId == null) {
                    ps.setNull(3, Types.INTEGER);
                } else {
                    ps.setInt(3, headUserId);
                }
                ps.setInt(4, department.getDepartmentId());
                ps.executeUpdate();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DatabaseException(DUPLICATE_MESSAGE, e);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update department: " + e.getMessage()
                    + " (Has database/migrations/002_department_head.sql been run?)", e);
        }
    }

    public void delete(int departmentId) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM departments WHERE department_id = ?")) {
            ps.setInt(1, departmentId);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DatabaseException("This department cannot be deleted because other records "
                    + "(courses, users, ...) still belong to it.", e);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete department: " + e.getMessage(), e);
        }
    }

    // ----- head of department (needs database/migrations/002_department_head.sql) -----

    /** The lecturer who heads a department. */
    public record DepartmentHead(int userId, String fullName) {
    }

    /** Department id -> head, only for departments that have one. */
    public Map<Integer, DepartmentHead> findHeads() throws DatabaseException {
        String sql = "SELECT d.department_id, u.user_id, u.full_name FROM departments d "
                + "JOIN users u ON u.user_id = d.head_user_id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Map<Integer, DepartmentHead> heads = new HashMap<>();
            while (rs.next()) {
                heads.put(rs.getInt("department_id"), new DepartmentHead(rs.getInt("user_id"), rs.getString("full_name")));
            }
            return heads;
        } catch (SQLException e) {
            throw new DatabaseException("Head of department is unavailable. Has "
                    + "database/migrations/002_department_head.sql been run? (" + e.getMessage() + ")", e);
        }
    }

    /** Sets (or clears, when userId is null) the head of a department. */
    public void setHead(int departmentId, Integer userId) throws DatabaseException {
        String sql = "UPDATE departments SET head_user_id = ? WHERE department_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, userId);
            ps.setInt(2, departmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to set the head of department. Has "
                    + "database/migrations/002_department_head.sql been run? (" + e.getMessage() + ")", e);
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