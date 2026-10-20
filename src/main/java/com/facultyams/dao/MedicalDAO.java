package com.facultyams.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.model.Medical;

public class MedicalDAO {
    public void addMedical(Medical m) {
        String sql = "INSERT INTO MEDICAL (attendance_id, reviewed_by, submitted_date, reason, approval_status) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, m.getAttendanceId());
            ps.setInt(2, m.getReviewedId());
            ps.setDate(3, java.sql.Date.valueOf(m.getSubmittedDate()));
            ps.setString(4, m.getReason());
            ps.setString(5, m.getApprovalStatus());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public List<Medical> getMedicalsByUser(int userId) {
        List<Medical> list = new ArrayList<>();
        String sql = "SELECT m.* FROM MEDICAL m "
                   + "JOIN ATTENDANCE a ON m.attendance_id = a.attendance_id "
                   + "WHERE a.user_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Medical m = new Medical(
                    rs.getInt("medical_id"),
                    rs.getInt("attendance_id"),
                    rs.getInt("reviewed_by"),
                    rs.getDate("submitted_date").toLocalDate(),
                    rs.getString("reason"),
                    rs.getString("approval_status")
                );
                list.add(m);
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return list;
    }

    public void updateApprovalStatus(int medicalId, String status, int reviewedBy) {
        if (!status.equals("pending") && !status.equals("approved") && !status.equals("rejected")) {
            throw new IllegalArgumentException("Approval status must be pending, approved or rejected");
        }
        String sql = "UPDATE MEDICAL SET approval_status = ?, reviewed_by = ? WHERE medical_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, reviewedBy);
            ps.setInt(3, medicalId);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}