
package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.model.Lecturer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LecturerDAO {

    public Lecturer findByLecturerID(int lecturerID) {

        String sql = "SELECT lecturerID, userID, designation, specialisation "
                + "FROM lecturers WHERE lecturerID = ?";

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
                            "",
                            "",
                            "",
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
}
