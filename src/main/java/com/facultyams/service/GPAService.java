package com.facultyams.service;

import com.facultyams.dao.MarksDAO;

import java.sql.SQLException;
import java.util.Map;

public class GPAService {

    private final MarksDAO marksDAO = new MarksDAO();

    // Calculate SGPA for each semester
    public Map<Integer, Double> calculateSGPA(
            String registrationNo) {

        validateRegistrationNo(registrationNo);

        try {
            return marksDAO.findSGPAByRegistrationNo(
                    registrationNo.trim()
            );
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to calculate SGPA.", e
            );
        }
    }

    // Calculate CGPA across stored course results
    public double calculateCGPA(String registrationNo) {

        validateRegistrationNo(registrationNo);

        try {
            double cgpa = marksDAO.findCGPAByRegistrationNo(
                    registrationNo.trim()
            );

            return Math.round(cgpa * 100.0) / 100.0;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to calculate CGPA.", e
            );
        }
    }

    private void validateRegistrationNo(String registrationNo) {
        if (registrationNo == null
                || registrationNo.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Please enter a student registration number."
            );
        }
    }
}

//calculations for SGPA and CGPA