package com.facultyams.service;

import com.facultyams.dao.MarksDAO;
import com.facultyams.dao.MarksDAO.StudentCourseResult;
import com.facultyams.exception.DatabaseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * SGPA / CGPA from course credits and grade points:
 * GPA = sum(gradePoints x credits) / sum(credits), rounded to 2 decimals.
 *
 * <ul>
 *   <li>SGPA is calculated per (academic level, semester) - Level 2 Semester 1 is never mixed
 *       with Level 1 Semester 1.</li>
 *   <li>CGPA covers every counted result of the student.</li>
 *   <li>Only PUBLISHED results count; drafts are reported separately and never silently included.</li>
 * </ul>
 * Not implemented because no rule is configured anywhere in the project: repeat-attempt grade
 * caps, non-GPA courses, and credit limits.
 */
public class GPAService {

    public record SemesterGpa(int level, int semester, double sgpa, int credits) {
        public String label() {
            return "Level " + level + " Semester " + semester;
        }
    }

    /** {@code cgpa} is null when the student has no published result yet. */
    public record GpaSummary(List<SemesterGpa> semesters, Double cgpa, int creditsCounted, int draftResults) {
    }

    private final LecturerAccessService access;
    private final MarksDAO marksDAO;

    public GPAService() {
        this(new LecturerAccessService(), new MarksDAO());
    }

    public GPAService(LecturerAccessService access, MarksDAO marksDAO) {
        this.access = access;
        this.marksDAO = marksDAO;
    }

    /** GPA of a student the logged-in lecturer is authorized to see. */
    public GpaSummary calculate(int studentId) throws DatabaseException {
        access.requireStudentAuthorized(studentId);
        return summarise(marksDAO.findStudentResults(studentId, access.currentLecturerId()));
    }

    /** Pure calculation over stored results (no database access). */
    public static GpaSummary summarise(List<StudentCourseResult> results) {
        Map<Integer, List<AcademicRules.CreditResult>> bySemester = new TreeMap<>();
        Map<Integer, int[]> keys = new TreeMap<>();
        List<AcademicRules.CreditResult> all = new ArrayList<>();
        int drafts = 0;
        int credits = 0;
        for (StudentCourseResult r : results) {
            if (r.overall() == null || r.gradePoints() == null) {
                continue;                       // no calculated result yet
            }
            if (!r.published()) {
                drafts++;
                continue;
            }
            AcademicRules.CreditResult cr = new AcademicRules.CreditResult(r.credits(), r.gradePoints());
            int key = r.level() * 10 + r.semester();
            bySemester.computeIfAbsent(key, k -> new ArrayList<>()).add(cr);
            keys.put(key, new int[]{r.level(), r.semester()});
            all.add(cr);
            credits += r.credits();
        }
        List<SemesterGpa> semesters = new ArrayList<>();
        for (Map.Entry<Integer, List<AcademicRules.CreditResult>> e : bySemester.entrySet()) {
            Double sgpa = AcademicRules.gpa(e.getValue());
            int sum = e.getValue().stream().mapToInt(AcademicRules.CreditResult::credits).sum();
            if (sgpa != null) {
                int[] k = keys.get(e.getKey());
                semesters.add(new SemesterGpa(k[0], k[1], sgpa, sum));
            }
        }
        return new GpaSummary(semesters, AcademicRules.gpa(all), credits, drafts);
    }
}
