package com.facultyams.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.model.Attendance;

import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;

public class AttendanceDAO {

    public void addAttendance(Attendance a) {
        String sql = "INSERT INTO ATTENDANCE (user_id, component_id, session_no, date, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
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

    public List<Attendance> getAttendanceByUser(int userId) {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT * FROM ATTENDANCE WHERE user_id=?";

        try (Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Attendance a = new Attendance(
                        rs.getInt("attendance_id"),
                        rs.getInt("user_id"),
                        rs.getInt("component_id"),
                        rs.getInt("session_no"),
                        rs.getDate("date").toLocalDate(),
                        rs.getString("status"));
                list.add(a);
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return list;
    }
}