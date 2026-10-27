package com.facultyams.service;

import com.facultyams.dao.MedicalDAO;
import com.facultyams.dao.TechnicalLookupDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.EvidenceRef;
import com.facultyams.model.Medical;
import com.facultyams.model.MedicalFilter;
import com.facultyams.model.MedicalRow;
import com.facultyams.model.SessionInfo;
import com.facultyams.model.StudentInfo;
import com.facultyams.model.SubmittableSession;
import com.facultyams.model.User;
import com.facultyams.security.EvidenceStorage;
import com.facultyams.security.Role;
import com.facultyams.security.Session;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Medical submission, review and reporting rules.
 *
 * <ul>
 *   <li>Submitting: a student (their own absences) or a technical officer (on behalf of a student)</li>
 *   <li>Reviewing, listing, reports and documents: technical officers only (a student may read their own)</li>
 *   <li>A medical may only be filed for an absent session and only once per session</li>
 *   <li>Approved medicals are counted by {@link AttendanceService} as attended sessions</li>
 * </ul>
 */
public class MedicalService {

    public static final String PENDING = "pending";
    public static final String APPROVED = "approved";
    public static final String REJECTED = "rejected";
    public static final int MIN_TEXT = 5;
    public static final int MAX_TEXT = 255;
    public static final int LIST_LIMIT = 5000;

    private final MedicalDAO dao;
    private final TechnicalLookupDAO lookupDAO;
    private final EvidenceStorage storage;

    public MedicalService() {
        this(new MedicalDAO(), new TechnicalLookupDAO(), new EvidenceStorage());
    }

    public MedicalService(MedicalDAO dao, TechnicalLookupDAO lookupDAO, EvidenceStorage storage) {
        this.dao = dao;
        this.lookupDAO = lookupDAO;
        this.storage = storage;
    }

    private static User requireOfficer() {
        Session.requireRole(Role.TECHNICAL_OFFICER);
        return Session.getCurrentUser();
    }

    /** Technical officer, or a student (who is then limited to their own data by the caller's checks). */
    private static User requireOfficerOrStudent() {
        Session.requireLogin();
        if (!Session.hasRole(Role.TECHNICAL_OFFICER) && !Session.hasRole(Role.STUDENT)) {
            throw new AuthorizationException("Only students and technical officers can use medicals.");
        }
        return Session.getCurrentUser();
    }

    // ------------------------------------------------------------------ submission

    public List<StudentInfo> searchStudents(String text) throws DatabaseException {
        requireOfficer();
        return lookupDAO.searchStudents(text, 50);
    }

    /** Absent sessions of the student that still have no medical. */
    public List<SubmittableSession> getSubmittableSessions(int studentId) throws DatabaseException {
        User me = requireOfficerOrStudent();
        requireOwnDataIfStudent(me, studentId);
        return dao.findSubmittable(studentId);
    }

    /** Files a pending medical for one absent session. */
    public int submit(int attendanceId, String reason, Path evidence) throws ValidationException, DatabaseException {
        return submit(List.of(attendanceId), reason, evidence);
    }

    /**
     * Files a pending medical for each of the given absent sessions (for example every absence in a date range)
     * in one transaction: either all are saved or none. The optional document is validated and stored once and
     * shared by the medicals.
     *
     * @return number of medicals created
     */
    public int submit(List<Integer> attendanceIds, String reason, Path evidence)
            throws ValidationException, DatabaseException {
        User me = requireOfficerOrStudent();
        String text = reason == null ? "" : reason.trim();
        if (text.length() < MIN_TEXT) {
            throw new ValidationException("Enter the reason (at least " + MIN_TEXT + " characters).");
        }
        if (text.length() > MAX_TEXT) {
            throw new ValidationException("The reason must be at most " + MAX_TEXT + " characters.");
        }
        if (attendanceIds == null || attendanceIds.isEmpty()) {
            throw new ValidationException("Select at least one absent session.");
        }
        Integer studentId = null;
        for (int id : new java.util.LinkedHashSet<>(attendanceIds)) {
            SessionInfo session = dao.findSession(id);
            if (session == null) {
                throw new ValidationException("A selected absence does not exist any more. Please reload the list.");
            }
            requireOwnDataIfStudent(me, session.userId());
            if (studentId != null && studentId != session.userId()) {
                throw new ValidationException("All sessions of one submission must belong to the same student.");
            }
            studentId = session.userId();
            if (!"absent".equals(session.status())) {
                throw new ValidationException("A medical can only be submitted for an absent session (" + session.date() + " is not absent).");
            }
            if (session.hasMedical()) {
                throw new ValidationException("A medical has already been submitted for the session on " + session.date() + ".");
            }
        }
        EvidenceStorage.Stored stored = evidence == null ? null : storage.store(evidence); // type, size, content checks
        List<Medical> medicals = new java.util.ArrayList<>();
        for (int id : new java.util.LinkedHashSet<>(attendanceIds)) {
            Medical m = new Medical(id, 0, LocalDate.now(), text, PENDING);
            m.setSubmittedBy(me.getUserId());
            if (stored != null) {
                m.setEvidenceName(stored.originalName());
                m.setEvidenceFile(stored.storedName());
            }
            medicals.add(m);
        }
        try {
            return dao.insertSubmissions(medicals);
        } catch (DatabaseException e) {
            if (stored != null) {
                storage.delete(stored.storedName());
            }
            throw e;
        }
    }

    private static void requireOwnDataIfStudent(User me, int studentId) {
        if (me.getRole() == Role.STUDENT && me.getUserId() != studentId) {
            throw new AuthorizationException("You can only use your own medicals.");
        }
    }

    // ------------------------------------------------------------------ listing

    /** All medicals matching the filter (technical officers only). */
    public List<MedicalRow> list(MedicalFilter filter) throws ValidationException, DatabaseException {
        requireOfficer();
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new ValidationException("The 'from' date must not be after the 'to' date.");
        }
        return dao.findMedicals(filter, LIST_LIMIT);
    }

    public List<MedicalRow> listPending() throws ValidationException, DatabaseException {
        return list(MedicalFilter.none().withStatus(PENDING));
    }

    /** A student's own submissions with their status; a technical officer may ask for any student. */
    public List<MedicalRow> listForStudent(int studentId) throws DatabaseException {
        User me = requireOfficerOrStudent();
        requireOwnDataIfStudent(me, studentId);
        return dao.findMedicals(new MedicalFilter(null, studentId, null, null, null, null, null), LIST_LIMIT);
    }

    public Map<String, Integer> countByStatus() throws DatabaseException {
        requireOfficer();
        return dao.countByStatus();
    }

    // ------------------------------------------------------------------ decisions

    public void approve(int medicalId) throws ValidationException, DatabaseException {
        decide(medicalId, APPROVED, null);
    }

    public void reject(int medicalId, String reason) throws ValidationException, DatabaseException {
        String text = reason == null ? "" : reason.trim();
        if (text.length() < MIN_TEXT) {
            throw new ValidationException("Enter the reason for rejecting (at least " + MIN_TEXT + " characters).");
        }
        if (text.length() > MAX_TEXT) {
            throw new ValidationException("The rejection reason must be at most " + MAX_TEXT + " characters.");
        }
        decide(medicalId, REJECTED, text);
    }

    private void decide(int medicalId, String status, String rejectionReason)
            throws ValidationException, DatabaseException {
        User officer = requireOfficer();
        String current = dao.statusOf(medicalId);
        if (current == null) {
            throw new ValidationException("Medical " + medicalId + " does not exist.");
        }
        if (!PENDING.equals(current)) {
            throw new ValidationException("This medical has already been " + current + ".");
        }
        // the UPDATE only matches a pending row, so two officers cannot both decide the same medical
        if (!dao.decide(medicalId, status, officer.getUserId(), rejectionReason)) {
            throw new ValidationException("This medical was decided by someone else just now. Please refresh.");
        }
    }

    // ------------------------------------------------------------------ documents

    /** File to open for a medical's document; technical officers, or the student the medical belongs to. */
    public Path getEvidencePath(int medicalId) throws ValidationException, DatabaseException {
        User me = requireOfficerOrStudent();
        EvidenceRef ref = dao.findEvidence(medicalId);
        if (ref == null) {
            throw new ValidationException("This medical has no supporting document.");
        }
        requireOwnDataIfStudent(me, ref.studentId());
        Path path = storage.resolve(ref.storedName());
        if (path == null) {
            throw new ValidationException("The document file is missing from storage. Ask the student to resubmit it.");
        }
        return path;
    }
}