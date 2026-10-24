package com.facultyams.service;

import com.facultyams.model.Marks;
import com.facultyams.dao.MarksDAO;

import java.sql.SQLException;
import java.util.List;

public class MarksService {

    private final MarksDAO marksDAO = new MarksDAO();

    // Validate marks between 0 and 100
    public boolean isValidateMarks(double marks) {
        return marks >= 0 && marks <= 100;
    }

    // CA eligibility: at least 40
    public boolean isCAEligible(double caMarks) {
        if (!isValidateMarks(caMarks)) {
            throw new IllegalArgumentException(
                    "CA marks must be between 0 and 100."
            );
        }

        return caMarks >= 40;
    }

    // Final exam eligibility requires CA >= 40 and attendance >= 80
    public boolean isExamEligiblity(
            double caMarks, double attendancePercentage) {

        if (!isValidateMarks(caMarks)) {
            throw new IllegalArgumentException(
                    "CA marks must be between 0 and 100."
            );
        }

        if (!isValidateMarks(attendancePercentage)) {
            throw new IllegalArgumentException(
                    "Attendance must be between 0 and 100."
            );
        }

        return isCAEligible(caMarks)
                && attendancePercentage >= 80;
    }

    // Calculate overall mark: 40% CA + 60% final examination
    public double calculateOverallMark(
            double caMarks, double finalMarks) {

        if (!isValidateMarks(caMarks)
                || !isValidateMarks(finalMarks)) {
            throw new IllegalArgumentException(
                    "CA and final marks must be between 0 and 100."
            );
        }

        double overall =
                (caMarks * 0.40) + (finalMarks * 0.60);

        return Math.round(overall * 100.0) / 100.0;
    }

    // Assign grade using the grade ranges in your SRS
    public String calculateGrade(double overallMark) {

        if (!isValidateMarks(overallMark)) {
            throw new IllegalArgumentException(
                    "Overall mark must be between 0 and 100."
            );
        }

        if (overallMark >= 85) return "A+";
        if (overallMark >= 75) return "A";
        if (overallMark >= 70) return "A-";
        if (overallMark >= 65) return "B+";
        if (overallMark >= 60) return "B";
        if (overallMark >= 55) return "B-";
        if (overallMark >= 50) return "C+";
        if (overallMark >= 45) return "C";
        if (overallMark >= 40) return "C-";
        if (overallMark >= 35) return "D+";
        if (overallMark >= 30) return "D";

        return "E";
    }

    // Assign grade points using the SRS grading table
    public double calculateGradePoints(double overallMark) {

        if (!isValidateMarks(overallMark)) {
            throw new IllegalArgumentException(
                    "Overall mark must be between 0 and 100."
            );
        }

        if (overallMark >= 85) return 4.0;
        if (overallMark >= 75) return 4.0;
        if (overallMark >= 70) return 3.7;
        if (overallMark >= 65) return 3.3;
        if (overallMark >= 60) return 3.0;
        if (overallMark >= 55) return 2.7;
        if (overallMark >= 50) return 2.3;
        if (overallMark >= 45) return 2.0;
        if (overallMark >= 40) return 1.7;
        if (overallMark >= 35) return 1.3;
        if (overallMark >= 30) return 1.0;

        return 0.0;
    }

    // Validate a Marks object
    public boolean validateMarksObject(Marks marks) {

        if (marks == null) {
            throw new IllegalArgumentException(
                    "Marks cannot be null."
            );
        }

        if (!isValidateMarks(marks.getObtainMarks())) {
            throw new IllegalArgumentException(
                    "CA marks must be between 0 and 100."
            );
        }

        if (!isValidateMarks(marks.getFinalMarks())) {
            throw new IllegalArgumentException(
                    "Final marks must be between 0 and 100."
            );
        }

        return true;
    }

    // Find a saved record by Marks ID
    public Marks findMarksById(int marksID) throws SQLException {

        if (marksID <= 0) {
            throw new IllegalArgumentException(
                    "Marks ID must be greater than zero."
            );
        }

        return marksDAO.findMarksById(marksID);
    }

    // Find saved results using a student's registration number
    public List<String> findMarksByRegistrationNo(
            String registrationNo) {

        if (registrationNo == null
                || registrationNo.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Enter a student registration number."
            );
        }

        try {
            return marksDAO.findMarksSummaryByRegistrationNo(
                    registrationNo.trim()
            );
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to retrieve student marks.", e
            );
        }
    }

    // Calculate grade and grade points before saving
    public void saveMarks(Marks marks) {

        validateMarksObject(marks);

        if (marks.getStudentId() <= 0
                || marks.getCourseId() <= 0) {
            throw new IllegalArgumentException(
                    "A valid student and course are required."
            );
        }

        double overallMark = calculateOverallMark(
                marks.getObtainMarks(),
                marks.getFinalMarks()
        );

        marks.setOverallMark(overallMark);
        marks.setGrade(calculateGrade(overallMark));
        marks.setGradePoints(
                calculateGradePoints(overallMark)
        );
        marks.setEligible(
                isCAEligible(marks.getObtainMarks())
        );

        try {
            marksDAO.saveMarks(marks);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to save marks.", e
            );
        }
    }
}