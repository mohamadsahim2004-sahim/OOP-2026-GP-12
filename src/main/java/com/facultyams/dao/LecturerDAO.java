
package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.model.Lecturer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LecturerDAO {

    public Lecturer findByLecturerID(int lecturerID) {

        String sql = "SELECT l.lecturerID, l.userID, l.designation, l.specialisation, "
                + "u.full_name, u.email, u.phone "
                + "FROM lecturers l "
                + "JOIN users u ON l.userID = u.user_id "
                + "WHERE l.lecturerID = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, lecturerID);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return new Lecturer(
                            resultSet.getInt("userID"),
                            "",
                            "",
                            "Lecturer",
                            resultSet.getString("full_name"),
                            resultSet.getString("email"),
                            resultSet.getString("phone"),
                            "",
                            0,
                            resultSet.getInt("lecturerID"),
                            resultSet.getString("designation"),
                            resultSet.getString("specialisation")
                    );
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to retrieve lecturer.", e);
        }

        return null;
    }
    public void updateProfile(int userID, String fullName, String email,
                              String phone, String profilePicture) {

        String sql = "UPDATE users SET full_name = ?, email = ?, "
                + "phone = ?, profilePicture = ? "
                + "WHERE user_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, fullName);
            statement.setString(2, email);
            statement.setString(3, phone);
            statement.setString(4, profilePicture);
            statement.setInt(5, userID);

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to update lecturer profile.", e);
        }
    }
}

