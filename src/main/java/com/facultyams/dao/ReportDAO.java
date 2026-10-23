package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection;
import com.facultyams.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Read-only aggregate queries for the Admin Reports screen. */
public class ReportDAO {

    /** Users of one role: how many exist and how many accounts are active. */
    public record RoleSummary(String role, int total, int active) {
    }

    /** One department with its number of courses and users. */
    public record DepartmentSummary(String code, String name, int courses, int users) {
    }

    /** Number of timetable slots on one weekday. */
    public record DaySummary(String day, int slots) {
    }

    public List<RoleSummary> usersByRole() throws DatabaseException {
        String sql = "SELECT role, COUNT(*) AS total, SUM(is_active) AS active FROM users "
                + "GROUP BY role ORDER BY FIELD(role,'ADMIN','LECTURER','TECHNICAL_OFFICER','STUDENT')";
        List<RoleSummary> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new RoleSummary(rs.getString("role"), rs.getInt("total"), rs.getInt("active")));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to build the users report.", e);
        }
    }

    public List<DepartmentSummary> departmentOverview() throws DatabaseException {
        String sql = "SELECT d.department_code, d.department_name, "
                + "(SELECT COUNT(*) FROM courses c WHERE c.department_id = d.department_id) AS courses, "
                + "(SELECT COUNT(*) FROM users u WHERE u.department_id = d.department_id) AS users "
                + "FROM departments d ORDER BY d.department_code";
        List<DepartmentSummary> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DepartmentSummary(rs.getString("department_code"), rs.getString("department_name"),
                        rs.getInt("courses"), rs.getInt("users")));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to build the departments report.", e);
        }
    }

    public List<DaySummary> timetableLoad() throws DatabaseException {
        String sql = "SELECT day_of_week, COUNT(*) AS slots FROM timetables GROUP BY day_of_week "
                + "ORDER BY FIELD(day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY')";
        List<DaySummary> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DaySummary(rs.getString("day_of_week"), rs.getInt("slots")));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to build the timetable report. Has "
                    + "database/migrations/001_notices_timetables.sql been run?", e);
        }
    }
}