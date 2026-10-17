package com.facultyams.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.model.Attendance;

public class AttendanceDAO {

    public void addAttendance(Attendance a) {
        String sql="INSERT INTO ATTENDANCE (user_id, component_id, session_no, date, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection con=DatabaseConnection.getConnection();
             PreparedStatement ps=con.prepareStatement(sql)) {
            ps.setInt(1, a.getUserId());
            ps.setInt(2, a.getComponentId());
            ps.setInt(3, a.getSessionNo());
            ps.setDate(4, java.sql.Date.valueOf(a.getDate()));
            ps.setString(5, a.getStatus());
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}