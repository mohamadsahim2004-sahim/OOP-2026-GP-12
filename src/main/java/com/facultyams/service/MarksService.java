package com.facultyams.service;

import com.facultyams.dao.EvaluationComponentDAO;
import com.facultyams.dao.LecturerAccessDAO.StudentRow;
import com.facultyams.dao.MarksDAO;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Course;
import com.facultyams.model.EvaluationComponent;
import com.facultyams.model.Marks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Marks entry: enter and update CA, assignment, practical and final-examination marks per
 * evaluation component. When a student has a mark for every component, the final result
 * (CA %, final %, overall, grade, grade points, eligibility) is calculated and stored in the same
 * database transaction as the marks.
 *
 * <p>Only the assigned lecturer of the course can enter marks, only for students of that course,
 * and a published result is locked. There is exactly one mark per student and component and one
 * result per student and course.
 */
public class MarksService {

    /** One student's line on the entry sheet. */
    public record EntryRow(StudentRow student, Map<Integer, Double> marks, boolean hasResult,
                           Double overall, String grade, boolean published) {
    }

    public record EntrySheet(Course course, List<EvaluationComponent> components,
                             AcademicRules.SchemeCheck scheme, List<EntryRow> rows) {
    }

    private final LecturerAccessService access;
    private final EvaluationComponentDAO componentDAO;
    private final MarksDAO marksDAO;

    public MarksService() {
        this(new LecturerAccessService(), new EvaluationComponentDAO(), new MarksDAO());
    }

    public MarksService(LecturerAccessService access, EvaluationComponentDAO componentDAO, MarksDAO marksDAO) {
        this.access = access;
        this.componentDAO = componentDAO;
        this.marksDAO = marksDAO;
    }

    public EntrySheet loadEntrySheet(int courseId) throws DatabaseException {
        Course course = access.requireOwnedCourse(courseId);
        List<EvaluationComponent> components = componentDAO.findByCourse(courseId);
        Map<Integer, Map<Integer, Double>> allMarks = componentDAO.findMarksByCourse(courseId);
        Map<Integer, MarksDAO.ResultRow> results = new HashMap<>();
        for (MarksDAO.ResultRow r : marksDAO.findResultsByCourse(courseId)) {
            results.put(r.studentId(), r);
        }
        List<EntryRow> rows = new ArrayList<>();
        for (StudentRow s : access.getStudents(courseId)) {
            MarksDAO.ResultRow r = results.get(s.studentId());
            rows.add(new EntryRow(s,
                    new LinkedHashMap<>(allMarks.getOrDefault(s.studentId(), Map.of())),
                    r != null && r.overall() != null,
                    r == null ? null : r.overall(),
                    r == null ? null : r.grade(),
                    r != null && r.published()));
        }
        return new EntrySheet(course, components, AcademicRules.checkScheme(components), rows);
    }

    /**
     * Saves the marks entered for one student and recalculates the result when complete.
     *
     * @param entered componentId -> mark; components left out keep their saved mark
     * @return the calculated result, or {@code null} while some component still has no mark
     */
    public AcademicRules.Result saveStudentMarks(int courseId, int studentId, Map<Integer, Double> entered)
            throws DatabaseException, ValidationException {
        int lecturerId = access.currentLecturerId();
        access.requireStudentInCourse(courseId, studentId);

        List<EvaluationComponent> components = componentDAO.findByCourse(courseId);
        AcademicRules.SchemeCheck scheme = AcademicRules.checkScheme(components);
        if (!scheme.complete()) {
            throw new ValidationException("The evaluation scheme of this course is incomplete: "
                    + scheme.problem());
        }
        if (entered == null || entered.isEmpty()) {
            throw new ValidationException("Enter at least one mark.");
        }

        Map<Integer, EvaluationComponent> byId = new HashMap<>();
        for (EvaluationComponent c : components) {
            byId.put(c.getEvalComponentId(), c);
        }
        Map<Integer, Double> validated = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> e : entered.entrySet()) {
            EvaluationComponent c = byId.get(e.getKey());
            if (c == null) {
                throw new ValidationException("A component does not belong to this course.");
            }
            if (e.getValue() == null) {
                throw new ValidationException(c.getComponentName() + ": enter a mark.");
            }
            validated.put(c.getEvalComponentId(), AcademicRules.requireValidMark(c, e.getValue()));
        }

        Marks existing = marksDAO.findResult(studentId, courseId);
        if (existing != null && existing.isPublished()) {
            throw new ValidationException(
                    "This result is published and locked. Unpublish the course results to change marks.");
        }

        Map<Integer, Double> merged = new HashMap<>(componentDAO.findMarksOfStudent(courseId, studentId));
        merged.putAll(validated);

        AcademicRules.Result result = AcademicRules.computeResult(components, merged,
                access.attendanceOf(access.attendanceByStudent(courseId), studentId));

        marksDAO.saveStudentMarks(courseId, studentId, lecturerId, validated,
                result == null ? null : toMarks(existing, studentId, courseId, result));
        return result;
    }

    /** Builds the row stored in the marks table. */
    static Marks toMarks(Marks existing, int studentId, int courseId, AcademicRules.Result r) {
        Marks marks = new Marks(existing == null ? 0 : existing.getMarksId(), studentId, courseId,
                r.caPercent(), r.finalPercent(), r.grade(), r.eligibility().eligible());
        marks.setOverallMark(r.overall());
        marks.setGradePoints(r.gradePoints());
        return marks;
    }
}
