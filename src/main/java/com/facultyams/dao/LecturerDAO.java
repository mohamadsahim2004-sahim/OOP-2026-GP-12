
package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.Lecturer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LecturerDAO {

    public Lecturer findByLecturerID(int lecturerID) {

        String sql =
                "SELECT l.user_id, l.designation, " +
                        "u.username, u.full_name, u.email, " +
                        "u.contact_number, u.profile_picture, u.department_id " +
                        "FROM lecturers l " +
                        "JOIN users u ON l.user_id = u.user_id " +
                        "WHERE l.user_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, lecturerID);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {

                    Lecturer lecturer = new Lecturer(
                            resultSet.getInt("user_id"),
                            resultSet.getString("username"),
                            resultSet.getString("full_name"),
                            resultSet.getString("email"),
                            resultSet.getString("designation")
                    );

                    lecturer.setPhone(
                            resultSet.getString("contact_number"));

                    lecturer.setProfilePicture(
                            resultSet.getString("profile_picture"));

                    lecturer.setDepartmentID(
                            resultSet.getInt("department_id"));

                    return lecturer;
                }
            }

        } catch (DatabaseException e) {
            throw new RuntimeException(
                    "Database connection failed while retrieving lecturer.", e);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to retrieve lecturer.", e);
        }

        return null;
    }

    public void updateProfile(
            int userID,
            String fullName,
            String email,
            String phone,
            String profilePicture) {

        String sql =
                "UPDATE users SET full_name = ?, email = ?, " +
                        "contact_number = ?, profile_picture = ? " +
                        "WHERE user_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, fullName);
            statement.setString(2, email);
            statement.setString(3, phone);
            statement.setString(4, profilePicture);
            statement.setInt(5, userID);

            statement.executeUpdate();

        } catch (DatabaseException e) {
            throw new RuntimeException(
                    "Database connection failed while updating lecturer profile.", e);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to update lecturer profile.", e);
        }
    }
}
