package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection; //to build connection between java and SQL db
import com.facultyams.model.Marks;
import java.sql.Connection; // to import connection
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class MarksDAO {
    private Connection connection;   //this creates a variable that hods the database connection

    public MarksDAO(){    //when MarksDAO object is created then this constructor gets the java-SQL connection
                          //when marksDA object is created
        connection =DatabaseConnection.getConnection();
    }
    public void saveMarks(Marks marks)throws SQLException {
        String sql = "INSERT INTO marks (marksid, obtainMarks, finalMarks, grade, isEligible) VALUES (?, ?, ?, ?, ?)";

        PreparedStatement statment = connection.prepareStatement(sql);
        statment.setInt(1, marks.getMarksId());
        statment.setDouble(2, marks.getObtainMarks());
        statment.setDouble(3, marks.getFinalMarks());
        statment.setString(4, marks.getGrade());
        statment.setBoolean(5, marks.isEligible());

        statment.executeUpdate();
    }

    public Marks findMarksById(int marksID) throws SQLException {
        String sql = "SELECT marksid, obtainMarks, finalMarks, grade, isEligible "
                + "FROM marks WHERE marksid = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, marksID);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Marks(
                            resultSet.getInt("marksid"),
                            resultSet.getDouble("obtainMarks"),
                            resultSet.getDouble("finalMarks"),
                            resultSet.getString("grade"),
                            resultSet.getBoolean("isEligible")
                    );
                }
            }
        }

        return null;
    }

}



