package com.facultyams.service;

import com.facultyams.dao.LecturerAccessDAO;
import com.facultyams.dao.LecturerAccessDAO.MedicalRow;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.Course;

import java.util.List;

/**
 * Medical records for the lecturer, with privacy controls:
 * <ul>
 *   <li>only records of absences in the lecturer's OWN courses are returned;</li>
 *   <li>the reason / diagnosis text is never loaded (the query does not select it) - a lecturer
 *       only needs to know whether the absence was excused;</li>
 *   <li>the screen is read-only: approving or rejecting is done by the technical officer.</li>
 * </ul>
 */
public class LecturerMedicalService {

    public static final String PRIVACY_NOTICE =
            "Medical details are confidential. Only the approval status is shown; the reason is not available to lecturers.";

    public record MedicalView(Course course, List<MedicalRow> rows, int pending, int approved, int rejected) {
    }

    private final LecturerAccessService access;
    private final LecturerAccessDAO dao;

    public LecturerMedicalService() {
        this(new LecturerAccessService(), new LecturerAccessDAO());
    }

    public LecturerMedicalService(LecturerAccessService access, LecturerAccessDAO dao) {
        this.access = access;
        this.dao = dao;
    }

    public MedicalView getMedicalRecords(int courseId) throws DatabaseException {
        Course course = access.requireOwnedCourse(courseId);
        List<MedicalRow> rows = dao.medicalRows(access.currentLecturerId(), courseId);
        int pending = 0;
        int approved = 0;
        int rejected = 0;
        for (MedicalRow r : rows) {
            switch (r.status() == null ? "" : r.status().toLowerCase()) {
                case "approved" -> approved++;
                case "rejected" -> rejected++;
                default -> pending++;
            }
        }
        return new MedicalView(course, rows, pending, approved, rejected);
    }
}
