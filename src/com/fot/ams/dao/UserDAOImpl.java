package com.fot.ams.dao;

import com.fot.ams.exception.DatabaseException;
import com.fot.ams.model.*;
import com.fot.ams.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of UserDAO demonstrating JDBC with PreparedStatements
 * as required by SRS Section 9, alongside a resilient in-memory seeded datastore
 * for offline demonstration and development reliability.
 */
public class UserDAOImpl implements UserDAO {

    private static final Map<String, User> seedStore = new ConcurrentHashMap<>();

    static {
        // Initialize seed demo accounts per SRS specifications and Group 12 members
        initSeedData();
    }

    private static void initSeedData() {
        // 1. Admin
        Admin admin = new Admin(
            1,
            "admin",
            PasswordHasher.hashPassword("admin123"),
            "System Administrator",
            "admin@fot.ruh.ac.lk",
            "+94 41 222 3344",
            "DICT"
        );
        registerSeedUser(admin);

        // 2. Lecturer
        Lecturer lecturer = new Lecturer(
            2,
            "lec_ict01",
            PasswordHasher.hashPassword("lec123"),
            "Dr. K. L. Perera",
            "perera@fot.ruh.ac.lk",
            "+94 77 123 4567",
            "DICT",
            "Senior Lecturer (Gr. I)",
            "Software Engineering & Object Oriented Systems"
        );
        registerSeedUser(lecturer);
        // Alias for easy testing
        seedStore.put("lecturer", lecturer);

        // 3. Technical Officer
        TechnicalOfficer to = new TechnicalOfficer(
            3,
            "to_ict01",
            PasswordHasher.hashPassword("to123"),
            "Mr. S. Fernando",
            "fernando.to@fot.ruh.ac.lk",
            "+94 71 987 6543",
            "DICT"
        );
        registerSeedUser(to);
        seedStore.put("techofficer", to);

        // 4. Undergraduates (Group 12 Members)
        Undergraduate ug1 = new Undergraduate(
            4,
            "TG/2024/2105",
            PasswordHasher.hashPassword("student123"),
            "Mohamad Shahim",
            "shahim.tg2105@fot.ruh.ac.lk",
            "+94 76 111 2222",
            "DICT",
            "TG/2024/2105",
            "B09",
            "Active",
            2024
        );
        registerSeedUser(ug1);
        seedStore.put("shahim", ug1);
        seedStore.put("student", ug1);

        Undergraduate ug2 = new Undergraduate(
            5,
            "TG/2024/2104",
            PasswordHasher.hashPassword("student123"),
            "M. Aamir",
            "aamir.tg2104@fot.ruh.ac.lk",
            "+94 76 222 3333",
            "DICT",
            "TG/2024/2104",
            "B09",
            "Active",
            2024
        );
        registerSeedUser(ug2);
        seedStore.put("aamir", ug2);

        Undergraduate ug3 = new Undergraduate(
            6,
            "TG/2024/2093",
            PasswordHasher.hashPassword("student123"),
            "F. Zumra",
            "zumra.tg2093@fot.ruh.ac.lk",
            "+94 76 333 4444",
            "DICT",
            "TG/2024/2093",
            "B09",
            "Active",
            2024
        );
        registerSeedUser(ug3);
        seedStore.put("zumra", ug3);

        Undergraduate ug4 = new Undergraduate(
            7,
            "TG/2024/2076",
            PasswordHasher.hashPassword("student123"),
            "M. Mariyam",
            "mariyam.tg2076@fot.ruh.ac.lk",
            "+94 76 444 5555",
            "DICT",
            "TG/2024/2076",
            "B09",
            "Active",
            2024
        );
        registerSeedUser(ug4);
        seedStore.put("mariyam", ug4);
    }

    private static void registerSeedUser(User user) {
        seedStore.put(user.getUsername().toLowerCase(), user);
    }

    @Override
    public User findByUsername(String username) throws DatabaseException {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        String cleanUsername = username.trim().toLowerCase();

        // 1. Try JDBC Prepared Statement if MySQL connection is active
        Connection conn = DBConnection.getConnection();
        if (conn != null) {
            String sql = "SELECT * FROM users WHERE LOWER(username) = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, cleanUsername);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUser(rs, conn);
                    }
                }
            } catch (SQLException e) {
                // Fall back to seed store if DB error occurs
            }
        }

        // 2. Lookup in local datastore
        return seedStore.get(cleanUsername);
    }

    @Override
    public User authenticate(String username, String rawPassword) throws DatabaseException {
        User user = findByUsername(username);
        if (user == null) {
            return null;
        }

        // Verify SHA-256 hash
        if (PasswordHasher.verify(rawPassword, user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    @Override
    public boolean usernameExists(String username) throws DatabaseException {
        return findByUsername(username) != null;
    }

    @Override
    public void registerUser(User user) throws DatabaseException {
        if (user == null || user.getUsername() == null) {
            throw new DatabaseException("Cannot register null user.");
        }

        if (user.getUserID() <= 0) {
            int nextId = seedStore.values().stream()
                .mapToInt(User::getUserID)
                .max()
                .orElse(10) + 1;
            user.setUserID(nextId);
        }

        // Try persisting to active MySQL database if available
        Connection conn = DBConnection.getConnection();
        if (conn != null) {
            try {
                conn.setAutoCommit(false);
                String userSql = "INSERT INTO users (userID, username, password, role, fullName, email, phone, departmentID) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(userSql)) {
                    stmt.setInt(1, user.getUserID());
                    stmt.setString(2, user.getUsername());
                    stmt.setString(3, user.getPasswordHash());
                    stmt.setString(4, user.getRole().name());
                    stmt.setString(5, user.getFullName());
                    stmt.setString(6, user.getEmail());
                    stmt.setString(7, user.getPhone());
                    stmt.setString(8, user.getDepartmentID());
                    stmt.executeUpdate();
                }

                // Child table insertion based on Polymorphism
                if (user instanceof Undergraduate) {
                    Undergraduate ug = (Undergraduate) user;
                    String ugSql = "INSERT INTO undergraduates (undergraduateID, indexNumber, batch, enrollmentStatus, registrationYear) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement ugStmt = conn.prepareStatement(ugSql)) {
                        ugStmt.setInt(1, ug.getUserID());
                        ugStmt.setString(2, ug.getIndexNumber());
                        ugStmt.setString(3, ug.getBatch());
                        ugStmt.setString(4, ug.getEnrollmentStatus());
                        ugStmt.setInt(5, ug.getRegistrationYear());
                        ugStmt.executeUpdate();
                    }
                } else if (user instanceof Lecturer) {
                    Lecturer lec = (Lecturer) user;
                    String lecSql = "INSERT INTO lecturers (lecturerID, designation, specialisation) VALUES (?, ?, ?)";
                    try (PreparedStatement lecStmt = conn.prepareStatement(lecSql)) {
                        lecStmt.setInt(1, lec.getUserID());
                        lecStmt.setString(2, lec.getDesignation());
                        lecStmt.setString(3, lec.getSpecialisation());
                        lecStmt.executeUpdate();
                    }
                } else if (user instanceof TechnicalOfficer) {
                    TechnicalOfficer to = (TechnicalOfficer) user;
                    String toSql = "INSERT INTO technical_officers (officerID, departmentID) VALUES (?, ?)";
                    try (PreparedStatement toStmt = conn.prepareStatement(toSql)) {
                        toStmt.setInt(1, to.getUserID());
                        toStmt.setString(2, to.getDepartmentID());
                        toStmt.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }

        // Always register in persistent runtime store
        registerSeedUser(user);
        if (user instanceof Undergraduate) {
            seedStore.put(((Undergraduate) user).getIndexNumber().toLowerCase(), user);
        }
    }

    @Override
    public List<User> getAllUsers() throws DatabaseException {
        return new ArrayList<>(seedStore.values());
    }

    @Override
    public void saveUser(User user) throws DatabaseException {
        if (user != null && user.getUsername() != null) {
            registerUser(user);
        }
    }

    private User mapResultSetToUser(ResultSet rs, Connection conn) throws SQLException {
        int id = rs.getInt("userID");
        String username = rs.getString("username");
        String hash = rs.getString("password");
        String roleStr = rs.getString("role");
        String name = rs.getString("fullName");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String dept = rs.getString("departmentID");

        UserRole role;
        try {
            role = UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            role = UserRole.UNDERGRADUATE;
        }

        switch (role) {
            case ADMIN:
                return new Admin(id, username, hash, name, email, phone, dept);
            case LECTURER:
                return new Lecturer(id, username, hash, name, email, phone, dept, "Lecturer", "Computer Technology");
            case TECHNICAL_OFFICER:
                return new TechnicalOfficer(id, username, hash, name, email, phone, dept);
            case UNDERGRADUATE:
            default:
                return new Undergraduate(id, username, hash, name, email, phone, dept, username, "B09", "Active", 2024);
        }
    }
}
