package com.facultyams.service;

import com.facultyams.dao.EvaluationComponentDAO;
import com.facultyams.dao.LecturerAccessDAO.StudentRow;
import com.facultyams.dao.MarksDAO;
import com.facultyams.dao.MarksDAO.StudentCourseResult;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Course;
import com.facultyams.model.EvaluationComponent;
import com.facultyams.model.Marks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CA / exam eligibility, final results, publishing, and student academic results for the
 * lecturer's own courses and students.
 */
public class ResultService {

    public record EligibilityRow(StudentRow student, AcademicRules.Eligibility eligibility) {
    }

    public record EligibilitySheet(Course course, AcademicRules.SchemeCheck scheme, List<EligibilityRow> rows) {
    }

    /** {@code result} is null while the student's marks are incomplete. */
    public record ResultLine(StudentRow student, int marksEntered, int componentCount,
                             MarksDAO.ResultRow result) {
        public boolean complete() {
            return result != null && result.overall() != null;
        }
    }

    public record ResultSheet(Course course, AcademicRules.SchemeCheck scheme, List<ResultLine> lines) {
        public int published() {
            return (int) lines.stream().filter(l -> l.complete() && l.result().published()).count();
        }

        public int drafts() {
            return (int) lines.stream().filter(l -> l.complete() && !l.result().published()).count();
        }

        public int incomplete() {
            return (int) lines.stream().filter(l -> !l.complete()).count();
        }

        /** Unpublished results whose eligibility is not confirmed (not eligible or pending). */
        public int draftsNotEligible() {
            return (int) lines.stream()
                    .filter(l -> l.complete() && !l.result().published() && !l.result().eligible()).count();
        }
    }

    public record PublishOutcome(int toPublish, int incomplete, int notConfirmedEligible) {
    }

    public record StudentResults(StudentRow student, List<StudentCourseResult> myCourses,
                                 GPAService.GpaSummary gpa) {
    }

    private final LecturerAccessService access;
    private final EvaluationComponentDAO componentDAO;
    private final MarksDAO marksDAO;
    private final GPAService gpaService;

    public ResultService() {
        LecturerAccessService access = new LecturerAccessService();
        MarksDAO marksDAO = new MarksDAO();
        this.access = access;
        this.componentDAO = new EvaluationComponentDAO();
        this.marksDAO = marksDAO;
        this.gpaService = new GPAService(access, marksDAO);
    }

    public ResultService(LecturerAccessService access, EvaluationComponentDAO componentDAO,
                         MarksDAO marksDAO, GPAService gpaService) {
        this.access = access;
        this.componentDAO = componentDAO;
        this.marksDAO = marksDAO;
        this.gpaService = gpaService;
    }

    // ------------------------------------------------------------------ eligibility

    /** CA % (from the CA/assignment/practical marks) and attendance for every student of the course. */
    public EligibilitySheet getEligibility(int courseId) throws DatabaseException {
        Course course = access.requireOwnedCourse(courseId);
        List<EvaluationComponent> components = componentDAO.findByCourse(courseId);
        List<EvaluationComponent> caParts = AcademicRules.caComponents(components);
        Map<Integer, Map<Integer, Double>> marks = componentDAO.findMarksByCourse(courseId);
        Map<Integer, AcademicRules.AttendanceInfo> attendance = access.attendanceByStudent(courseId);

        List<EligibilityRow> rows = new ArrayList<>();
        for (StudentRow s : access.getStudents(courseId)) {
            Double ca = AcademicRules.percentOf(caParts, marks.getOrDefault(s.studentId(), Map.of()));
            rows.add(new EligibilityRow(s,
                    AcademicRules.eligibility(ca, access.attendanceOf(attendance, s.studentId()))));
        }
        return new EligibilitySheet(course, AcademicRules.checkScheme(components), rows);
    }

    // ------------------------------------------------------------------ final results

    public ResultSheet getCourseResults(int courseId) throws DatabaseException {
        Course course = access.requireOwnedCourse(courseId);
        List<EvaluationComponent> components = componentDAO.findByCourse(courseId);
        Map<Integer, Map<Integer, Double>> marks = componentDAO.findMarksByCourse(courseId);
        Map<Integer, MarksDAO.ResultRow> results = new HashMap<>();
        for (MarksDAO.ResultRow r : marksDAO.findResultsByCourse(courseId)) {
            results.put(r.studentId(), r);
        }
        List<ResultLine> lines = new ArrayList<>();
        for (StudentRow s : access.getStudents(courseId)) {
            lines.add(new ResultLine(s, marks.getOrDefault(s.studentId(), Map.of()).size(),
                    components.size(), results.get(s.studentId())));
        }
        return new ResultSheet(course, AcademicRules.checkScheme(components), lines);
    }

    /** What publishing would do right now (nothing is changed). */
    public PublishOutcome previewPublish(int courseId) throws DatabaseException {
        ResultSheet sheet = getCourseResults(courseId);
        return new PublishOutcome(sheet.drafts(), sheet.incomplete(), sheet.draftsNotEligible());
    }

    /** Publishes every calculated draft result of the course; incomplete students are skipped. */
    public PublishOutcome publish(int courseId) throws DatabaseException, ValidationException {
        PublishOutcome preview = previewPublish(courseId);
        if (preview.toPublish() == 0) {
            throw new ValidationException("There are no calculated results waiting to be published.");
        }
        marksDAO.publish(courseId);
        return preview;
    }

    /** Takes the course's published results back to draft so marks can be corrected. */
    public int unpublish(int courseId) throws DatabaseException {
        access.requireOwnedCourse(courseId);
        return marksDAO.unpublish(courseId);
    }

    /**
     * Recalculates the stored results of students whose marks are complete (for example after
     * attendance or medical approvals changed). Published results are never touched.
     *
     * @return number of results recalculated
     */
    public int recalculate(int courseId) throws DatabaseException {
        int lecturerId = access.currentLecturerId();
        access.requireOwnedCourse(courseId);
        List<EvaluationComponent> components = componentDAO.findByCourse(courseId);
        if (!AcademicRules.checkScheme(components).complete()) {
            return 0;
        }
        Map<Integer, Map<Integer, Double>> marks = componentDAO.findMarksByCourse(courseId);
        Map<Integer, AcademicRules.AttendanceInfo> attendance = access.attendanceByStudent(courseId);
        int updated = 0;
        for (StudentRow s : access.getStudents(courseId)) {
            Map<Integer, Double> own = marks.get(s.studentId());
            if (own == null) {
                continue;                        // legacy result without component marks: leave alone
            }
            AcademicRules.Result result = AcademicRules.computeResult(components, own,
                    access.attendanceOf(attendance, s.studentId()));
            Marks existing = marksDAO.findResult(s.studentId(), courseId);
            if (result == null || (existing != null && existing.isPublished())) {
                continue;
            }
            marksDAO.saveStudentMarks(courseId, s.studentId(), lecturerId, Map.of(),
                    MarksService.toMarks(existing, s.studentId(), courseId, result));
            updated++;
        }
        return updated;
    }

    // ------------------------------------------------------------------ student results

    public List<StudentRow> searchStudents(String text) throws DatabaseException, ValidationException {
        return access.searchStudents(text);
    }

    /**
     * Results of a student the lecturer is authorized to see. Course details are limited to the
     * lecturer's own courses; the GPA is the student's overall GPA (published results only).
     */
    public StudentResults getStudentResults(StudentRow student) throws DatabaseException {
        access.requireStudentAuthorized(student.studentId());
        List<StudentCourseResult> all = marksDAO.findStudentResults(student.studentId(), access.currentLecturerId());
        List<StudentCourseResult> mine = new ArrayList<>();
        for (StudentCourseResult r : all) {
            if (r.ownedByLecturer()) {
                mine.add(r);
            }
        }
        return new StudentResults(student, mine, GPAService.summarise(all));
    }
}
