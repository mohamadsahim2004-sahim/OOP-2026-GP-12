package com.facultyams.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.Attendance;

public class AttendanceDAO {

    public boolean addAttendance(Attendance a) {
        String sql = "INSERT INTO session_attendance (user_id, component_id, session_no, date, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, a.getUserId());
            ps.setInt(2, a.getComponentId());
            ps.setInt(3, a.getSessionNo());
            ps.setDate(4, java.sql.Date.valueOf(a.getDate()));
            ps.setString(5, a.getStatus());
            ps.executeUpdate();
            return true;

        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    public List<Attendance> getAttendanceByUser(int userId) {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT * FROM session_attendance WHERE user_id = ?";

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
                    rs.getString("status")
                );
                list.add(a);
            }

        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return list;
    }

    public int countPresent(int userId, int componentId) {
        String sql = "SELECT COUNT(*) FROM session_attendance "
                   + "WHERE user_id = ? AND component_id = ? AND status = 'present'";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, componentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return 0;
    }

    public List<Integer> getStudentIdsByComponent(int componentId) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT DISTINCT user_id FROM session_attendance WHERE component_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, componentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt("user_id"));
            }

        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return ids;
    }

    public boolean updateStatus(int attendanceId, String status) {
        if (!status.equals("present") && !status.equals("absent")) {
            throw new IllegalArgumentException("Status must be present or absent");
        }
        String sql = "UPDATE session_attendance SET status = ? WHERE attendance_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, attendanceId);
            ps.executeUpdate();
            return true;

        } catch (SQLException | DatabaseException e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }
}