package com.facultyams.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.facultyams.dao.AttendanceDAO;
import com.facultyams.dao.MedicalDAO;
import com.facultyams.model.AttendanceSummary;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.facultyams.dao.TechnicalLookupDAO;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.AttendanceFilter;
import com.facultyams.model.AttendanceRow;
import com.facultyams.model.ComponentInfo;
import com.facultyams.model.CourseInfo;
import com.facultyams.model.SheetRow;
import com.facultyams.model.StudentSummaryRow;
import com.facultyams.model.SummaryCounts;
import com.facultyams.security.Role;
import com.facultyams.security.Session;

public class AttendanceService {

    public static final int TOTAL_SESSIONS = 15;

    /** Statuses a session can be marked with. A late student counts as attended (see {@link #calculatePercentage}). */
    public static final String PRESENT = "present";
    public static final String ABSENT = "absent";
    public static final String LATE = "late";
    public static final List<String> STATUSES = List.of(PRESENT, ABSENT, LATE);

    /** Largest number of history rows loaded at once. */
    public static final int HISTORY_LIMIT = 5000;
    private static final double ELIGIBLE_AT = 80;

    private AttendanceDAO attendanceDAO = new AttendanceDAO();
    private MedicalDAO medicalDAO = new MedicalDAO();
    private TechnicalLookupDAO lookupDAO = new TechnicalLookupDAO();

    public double calculatePercentage(int present, int approvedMedical, int totalSessions) {
        if (totalSessions <= 0) {
            throw new IllegalArgumentException("Total sessions must be greater than 0");
        }
        return 100.0 * (present + approvedMedical) / totalSessions;
    }

    public boolean isEligible(double percentage) {
        return percentage >= ELIGIBLE_AT;
    }

    public double getStudentPercentage(int userId, int componentId) {
        int attended = attendanceDAO.countPresent(userId, componentId) + attendanceDAO.countLate(userId, componentId);
        int approved = medicalDAO.countApproved(userId, componentId);
        return calculatePercentage(attended, approved, TOTAL_SESSIONS);
    }

    public double getCombinedPercentage(int userId, int theoryId, int practicalId) {
        int present = attendanceDAO.countPresent(userId, theoryId) + attendanceDAO.countLate(userId, theoryId)
                    + attendanceDAO.countPresent(userId, practicalId) + attendanceDAO.countLate(userId, practicalId);
        int approved = medicalDAO.countApproved(userId, theoryId)
                     + medicalDAO.countApproved(userId, practicalId);
        return calculatePercentage(present, approved, TOTAL_SESSIONS * 2);
    }

    public List<AttendanceSummary> getBatchSummary(int componentId) {
        List<AttendanceSummary> list = new ArrayList<>();
        List<Integer> ids = attendanceDAO.getStudentIdsByComponent(componentId);

        for (int id : ids) {
            double percentage = getStudentPercentage(id, componentId);
            boolean eligible = isEligible(percentage);
            list.add(new AttendanceSummary(id, percentage, eligible));
        }
        return list;
    }
    //  how many sessions this student attended (used by the summary page)
    public int getPresentCount(int userId, int componentId) {
        return attendanceDAO.countPresent(userId, componentId) + attendanceDAO.countLate(userId, componentId);
    }

    // how many absent sessions were covered by an approved medical
    public int getApprovedCount(int userId, int componentId) {
        return medicalDAO.countApproved(userId, componentId);
    }

    // ------------------------------------------------------------------ Technical Officer features

    private static void requireOfficer() {
        Session.requireRole(Role.TECHNICAL_OFFICER);
    }

    public List<CourseInfo> getCourses() throws DatabaseException {
        requireOfficer();
        return lookupDAO.courses();
    }

    public List<String> getBatches() throws DatabaseException {
        requireOfficer();
        return lookupDAO.batches();
    }

    /** Components (Theory / Practical) of a course; the ones its course type needs are created if missing. */
    public List<ComponentInfo> getComponents(int courseId) throws DatabaseException {
        requireOfficer();
        return lookupDAO.ensureComponents(courseId);
    }

    /** Students of the batch with what is already saved for this component and session. */
    public List<SheetRow> loadSheet(int componentId, int sessionNo, String batch)
            throws ValidationException, DatabaseException {
        requireOfficer();
        checkSession(sessionNo);
        if (batch == null || batch.isBlank()) {
            throw new ValidationException("Select a batch.");
        }
        return attendanceDAO.loadSheet(componentId, sessionNo, batch);
    }

    /**
     * Saves (inserts or updates) the whole session in one transaction.
     * Duplicate rows cannot occur: a student has at most one row per component and session number.
     *
     * @param statusByStudent student id to present / absent / late
     */
    public int saveSession(int componentId, int sessionNo, String batch, LocalDate date,
                           Map<Integer, String> statusByStudent) throws ValidationException, DatabaseException {
        requireOfficer();
        checkSession(sessionNo);
        if (date == null) {
            throw new ValidationException("Select the session date.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("The session date cannot be in the future.");
        }
        if (statusByStudent == null || statusByStudent.isEmpty()) {
            throw new ValidationException("There are no students to save.");
        }
        Set<Integer> roster = lookupDAO.studentsInBatch(batch).stream()
                .map(st -> st.userId()).collect(Collectors.toSet());
        for (Map.Entry<Integer, String> e : statusByStudent.entrySet()) {
            if (!roster.contains(e.getKey())) {
                throw new ValidationException("Student " + e.getKey() + " is not an active member of batch " + batch + ".");
            }
            if (e.getValue() == null || !STATUSES.contains(e.getValue())) {
                throw new ValidationException("Every student must be marked Present, Absent or Late.");
            }
        }
        return attendanceDAO.saveSession(componentId, sessionNo, date, statusByStudent);
    }

    private static void checkSession(int sessionNo) throws ValidationException {
        if (sessionNo < 1 || sessionNo > TOTAL_SESSIONS) {
            throw new ValidationException("Session number must be between 1 and " + TOTAL_SESSIONS + ".");
        }
    }

    public List<AttendanceRow> getHistory(AttendanceFilter filter) throws ValidationException, DatabaseException {
        requireOfficer();
        checkRange(filter);
        return attendanceDAO.findHistory(filter, HISTORY_LIMIT);
    }

    /** Which components a summary covers (PDF: theory only, practical only, or combined). */
    public enum ComponentView {
        ALL_ROWS("Theory and practical (separate rows)"), THEORY("Theory only"), PRACTICAL("Practical only"),
        COMBINED("Combined (theory + practical)");

        private final String label;

        ComponentView(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Type shown on a combined summary row. */
    public static final String COMBINED_TYPE = "combined";

    /** Summary with theory and practical as separate rows (what the reports use). */
    public List<StudentSummaryRow> getSummary(AttendanceFilter filter) throws ValidationException, DatabaseException {
        return getSummary(filter, ComponentView.ALL_ROWS);
    }

    /**
     * Per-student attendance summary for the chosen component view.
     * THEORY / PRACTICAL keep only that component. COMBINED adds the components of a course together and
     * measures them against 15 sessions per component (or the sessions recorded when a date range is set).
     */
    public List<StudentSummaryRow> getSummary(AttendanceFilter filter, ComponentView view)
            throws ValidationException, DatabaseException {
        requireOfficer();
        checkRange(filter);
        ComponentView v = view == null ? ComponentView.ALL_ROWS : view;
        List<SummaryCounts> counts = attendanceDAO.findSummaryCounts(filter);
        List<StudentSummaryRow> rows = new ArrayList<>();
        if (v == ComponentView.COMBINED) {
            Map<String, Integer> componentsOf = lookupDAO.componentCountsByCourseCode();
            Map<String, int[]> sums = new LinkedHashMap<>();
            Map<String, SummaryCounts> first = new LinkedHashMap<>();
            for (SummaryCounts c : counts) {
                String key = c.userId() + "|" + c.courseCode();
                first.putIfAbsent(key, c);
                int[] a = sums.computeIfAbsent(key, k -> new int[5]);
                a[0] += c.present();
                a[1] += c.late();
                a[2] += c.absent();
                a[3] += c.approvedMedical();
                a[4] += c.recorded();
            }
            for (Map.Entry<String, int[]> e : sums.entrySet()) {
                SummaryCounts c = first.get(e.getKey());
                int[] a = e.getValue();
                int components = Math.max(1, componentsOf.getOrDefault(c.courseCode(), 1));
                int basis = filter.hasDateRange() ? a[4] : TOTAL_SESSIONS * components;
                rows.add(buildRow(c, 0, COMBINED_TYPE, a[0], a[1], a[2], a[3], a[4], basis));
            }
            return rows;
        }
        for (SummaryCounts c : counts) {
            if (v == ComponentView.THEORY && !"theory".equalsIgnoreCase(c.type())) {
                continue;
            }
            if (v == ComponentView.PRACTICAL && !"practical".equalsIgnoreCase(c.type())) {
                continue;
            }
            int basis = filter.hasDateRange() ? c.recorded() : TOTAL_SESSIONS;
            rows.add(buildRow(c, c.componentId(), c.type(), c.present(), c.late(), c.absent(),
                    c.approvedMedical(), c.recorded(), basis));
        }
        return rows;
    }

    private StudentSummaryRow buildRow(SummaryCounts c, int componentId, String type, int present, int late,
                                       int absent, int approvedMedical, int recorded, int basis) {
        double pct = basis <= 0 ? 0 : Math.min(100.0, calculatePercentage(present + late, approvedMedical, basis));
        return new StudentSummaryRow(c.userId(), c.fullName(), c.registrationNo(), c.batch(), componentId,
                c.courseCode(), c.courseName(), type, present, late, absent, approvedMedical, recorded, basis,
                pct, isEligible(pct));
    }

    private static void checkRange(AttendanceFilter f) throws ValidationException {
        if (f.from() != null && f.to() != null && f.from().isAfter(f.to())) {
            throw new ValidationException("The 'from' date must not be after the 'to' date.");
        }
    }

    /** Corrects the status of one saved attendance record (technical officers only). */
    public void correctStatus(int attendanceId, String status) throws ValidationException, DatabaseException {
        requireOfficer();
        if (status == null || !STATUSES.contains(status)) {
            throw new ValidationException("Choose Present, Absent or Late.");
        }
        if (!attendanceDAO.updateStatusChecked(attendanceId, status)) {
            throw new ValidationException("That attendance record no longer exists. Please search again.");
        }
    }
}