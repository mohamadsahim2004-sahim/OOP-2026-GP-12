package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.Admin;
import com.facultyams.model.Lecturer;
import com.facultyams.model.Student;
import com.facultyams.model.TechnicalOfficer;
import com.facultyams.model.User;
import com.facultyams.security.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Database access for users and their role-specific tables (lecturers, students).
 * Contains SQL only - no validation or business rules (see UserService).
 */
public class UserDAO {

    private static final String SELECT_USER =
            "SELECT u.user_id, u.username, u.password_hash, u.full_name, u.email, u.role, "
                    + "u.contact_number, u.profile_picture, u.department_id, u.is_active, u.created_at, "
                    + "d.department_name, l.designation, s.registration_no, s.batch, s.student_status "
                    + "FROM users u "
                    + "LEFT JOIN departments d ON d.department_id = u.department_id "
                    + "LEFT JOIN lecturers l ON l.user_id = u.user_id "
                    + "LEFT JOIN students s ON s.user_id = u.user_id ";

    // ------------------------------------------------------------------ queries

    public User findById(int userId) throws DatabaseException {
        return findOne(SELECT_USER + "WHERE u.user_id = ?", userId);
    }

    public User findByUsername(String username) throws DatabaseException {
        return findOne(SELECT_USER + "WHERE u.username = ?", username);
    }

    public List<User> findAll() throws DatabaseException {
        return findMany(SELECT_USER + "ORDER BY u.role, u.full_name");
    }

    /** All users of one role, e.g. all students - useful for other modules. */
    public List<User> findByRole(Role role) throws DatabaseException {
        return findMany(SELECT_USER + "WHERE u.role = ? ORDER BY u.full_name", role.name());
    }

    /**
     * Searches username, full name, email and registration number.
     *
     * @param keyword    text to search for (null/empty = all)
     * @param roleFilter role to filter by (null = all roles)
     */
    public List<User> search(String keyword, Role roleFilter) throws DatabaseException {
        StringBuilder sql = new StringBuilder(SELECT_USER).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append("AND (u.username LIKE ? OR u.full_name LIKE ? OR u.email LIKE ? OR s.registration_no LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            for (int i = 0; i < 4; i++) {
                params.add(like);
            }
        }
        if (roleFilter != null) {
            sql.append("AND u.role = ? ");
            params.add(roleFilter.name());
        }
        sql.append("ORDER BY u.role, u.full_name");
        return findMany(sql.toString(), params.toArray());
    }

    public boolean usernameExists(String username, int excludeUserId) throws DatabaseException {
        return exists("SELECT 1 FROM users WHERE username = ? AND user_id <> ?", username, excludeUserId);
    }

    public boolean emailExists(String email, int excludeUserId) throws DatabaseException {
        return exists("SELECT 1 FROM users WHERE email = ? AND user_id <> ?", email, excludeUserId);
    }

    public boolean registrationNumberExists(String registrationNo, int excludeUserId) throws DatabaseException {
        return exists("SELECT 1 FROM students WHERE registration_no = ? AND user_id <> ?",
                registrationNo, excludeUserId);
    }

    /** Number of users per role (all roles present in the map, 0 if none). */
    public Map<Role, Integer> countByRole() throws DatabaseException {
        Map<Role, Integer> counts = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            counts.put(role, 0);
        }
        String sql = "SELECT role, COUNT(*) AS total FROM users GROUP BY role";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(Role.fromDatabaseValue(rs.getString("role")), rs.getInt("total"));
            }
            return counts;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count users.", e);
        }
    }

    // ------------------------------------------------------------------ changes

    /**
     * Inserts the user (and lecturer/student row) in one transaction.
     * The password hash must already be set on the user.
     *
     * @return the generated user id (also set on the object)
     */
    public int insert(User user) throws DatabaseException {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, role, contact_number, "
                + "profile_picture, department_id, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPasswordHash());
                ps.setString(3, user.getFullName());
                ps.setString(4, user.getEmail());
                ps.setString(5, user.getRole().name());
                ps.setString(6, user.getContactNumber());
                ps.setString(7, user.getProfilePicture());
                setNullableInt(ps, 8, user.getDepartmentId());
                ps.setBoolean(9, user.isActive());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("No user id was generated.");
                    }
                    user.setUserId(keys.getInt(1));
                }
                saveRoleDetails(con, user);
                con.commit();
                return user.getUserId();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create user: " + e.getMessage(), e);
        }
    }

    /**
     * Updates common fields and role-specific details (role may change).
     * Does NOT change the password - use updatePassword().
     */
    public void update(User user) throws DatabaseException {
        String sql = "UPDATE users SET username = ?, full_name = ?, email = ?, role = ?, contact_number = ?, "
                + "profile_picture = ?, department_id = ?, is_active = ? WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getFullName());
                ps.setString(3, user.getEmail());
                ps.setString(4, user.getRole().name());
                ps.setString(5, user.getContactNumber());
                ps.setString(6, user.getProfilePicture());
                setNullableInt(ps, 7, user.getDepartmentId());
                ps.setBoolean(8, user.isActive());
                ps.setInt(9, user.getUserId());
                if (ps.executeUpdate() == 0) {
                    throw new SQLException("User " + user.getUserId() + " does not exist.");
                }
                saveRoleDetails(con, user);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update user: " + e.getMessage(), e);
        }
    }

    /** Updates only the self-editable profile fields (used by "My Profile" screens). */
    public void updateProfile(User user) throws DatabaseException {
        String sql = "UPDATE users SET full_name = ?, email = ?, contact_number = ?, profile_picture = ? "
                + "WHERE user_id = ?";
        executeUpdate(sql, "Failed to update profile.",
                user.getFullName(), user.getEmail(), user.getContactNumber(), user.getProfilePicture(),
                user.getUserId());
    }

    public void updatePassword(int userId, String passwordHash) throws DatabaseException {
        executeUpdate("UPDATE users SET password_hash = ? WHERE user_id = ?",
                "Failed to change password.", passwordHash, userId);
    }

    public void setActive(int userId, boolean active) throws DatabaseException {
        executeUpdate("UPDATE users SET is_active = ? WHERE user_id = ?",
                "Failed to change account status.", active, userId);
    }

    /**
     * Permanently deletes a user. Fails with a DatabaseException if other
     * records (marks, attendance, ...) still reference the user.
     */
    public void delete(int userId) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DatabaseException("This user cannot be deleted because other records still refer to "
                    + "them. Deactivate the account instead.", e);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Inserts or updates the lecturer/student row for this user, and removes a
     * row of the other kind if the role was changed. Uses upserts so existing
     * rows referenced by other modules (attendance, courses, ...) are kept.
     */
    private void saveRoleDetails(Connection con, User user) throws SQLException {
        if (user instanceof Lecturer) {
            Lecturer lecturer = (Lecturer) user;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO lecturers (user_id, designation) VALUES (?, ?) "
                            + "ON DUPLICATE KEY UPDATE designation = VALUES(designation)")) {
                ps.setInt(1, lecturer.getUserId());
                ps.setString(2, lecturer.getDesignation());
                ps.executeUpdate();
            }
        } else {
            deleteRow(con, "DELETE FROM lecturers WHERE user_id = ?", user.getUserId());
        }

        if (user instanceof Student) {
            Student student = (Student) user;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO students (user_id, registration_no, batch, student_status) VALUES (?, ?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE registration_no = VALUES(registration_no), "
                            + "batch = VALUES(batch), student_status = VALUES(student_status)")) {
                ps.setInt(1, student.getUserId());
                ps.setString(2, student.getRegistrationNumber());
                ps.setString(3, student.getBatch());
                ps.setString(4, student.getStudentStatus().name());
                ps.executeUpdate();
            }
        } else {
            deleteRow(con, "DELETE FROM students WHERE user_id = ?", user.getUserId());
        }
    }

    private static void deleteRow(Connection con, String sql, int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private User findOne(String sql, Object... params) throws DatabaseException {
        List<User> users = findMany(sql, params);
        return users.isEmpty() ? null : users.get(0);
    }

    private List<User> findMany(String sql, Object... params) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
                return users;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to read users.", e);
        }
    }

    private boolean exists(String sql, Object... params) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check user data.", e);
        }
    }

    private void executeUpdate(String sql, String errorMessage, Object... params) throws DatabaseException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(errorMessage + " " + e.getMessage(), e);
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    /** Factory: builds the correct User subclass from the role column (polymorphism). */
    private User mapRow(ResultSet rs) throws SQLException {
        Role role = Role.fromDatabaseValue(rs.getString("role"));
        User user;
        switch (role) {
            case ADMIN:
                user = new Admin();
                break;
            case LECTURER:
                Lecturer lecturer = new Lecturer();
                String designation = rs.getString("designation");
                if (designation != null) {
                    lecturer.setDesignation(designation);
                }
                user = lecturer;
                break;
            case TECHNICAL_OFFICER:
                user = new TechnicalOfficer();
                break;
            case STUDENT:
                Student student = new Student();
                student.setRegistrationNumber(rs.getString("registration_no"));
                student.setBatch(rs.getString("batch"));
                String status = rs.getString("student_status");
                if (status != null) {
                    student.setStudentStatus(Student.StudentStatus.valueOf(status));
                }
                user = student;
                break;
            default:
                throw new SQLException("Unknown role: " + role);
        }
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setContactNumber(rs.getString("contact_number"));
        user.setProfilePicture(rs.getString("profile_picture"));
        int departmentId = rs.getInt("department_id");
        user.setDepartmentId(rs.wasNull() ? null : departmentId);
        user.setDepartmentName(rs.getString("department_name"));
        user.setActive(rs.getBoolean("is_active"));
        Timestamp created = rs.getTimestamp("created_at");
        user.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return user;
    }
}