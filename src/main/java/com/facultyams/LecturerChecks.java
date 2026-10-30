package com.facultyams;

import com.facultyams.dao.CourseDAO;
import com.facultyams.dao.CourseMaterialDAO;
import com.facultyams.dao.EvaluationComponentDAO;
import com.facultyams.dao.LecturerAccessDAO;
import com.facultyams.dao.LecturerAccessDAO.AttendanceAgg;
import com.facultyams.dao.LecturerAccessDAO.MedicalRow;
import com.facultyams.dao.LecturerAccessDAO.StudentRow;
import com.facultyams.dao.LecturerDAO;
import com.facultyams.dao.MarksDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Course;
import com.facultyams.model.CourseMaterial;
import com.facultyams.model.EvaluationComponent;
import com.facultyams.model.Lecturer;
import com.facultyams.model.Marks;
import com.facultyams.model.Student;
import com.facultyams.model.User;
import com.facultyams.security.Session;
import com.facultyams.service.AcademicRules;
import com.facultyams.service.AcademicRules.AttendanceInfo;
import com.facultyams.service.AttendanceService;
import com.facultyams.service.CourseMaterialService;
import com.facultyams.service.EvaluationComponentService;
import com.facultyams.service.GPAService;
import com.facultyams.service.LecturerAccessService;
import com.facultyams.service.LecturerService;
import com.facultyams.service.MarksService;
import com.facultyams.service.LecturerMedicalService;
import com.facultyams.service.ResultService;
import com.facultyams.ui.lecturer.CourseMaterialsPanel;
import com.facultyams.ui.lecturer.DashboardHomePanel;
import com.facultyams.ui.lecturer.EligibilityPanel;
import com.facultyams.ui.lecturer.EvaluationComponentsPanel;
import com.facultyams.ui.lecturer.LecturerAttendancePanel;
import com.facultyams.ui.lecturer.LecturerMedicalPanel;
import com.facultyams.ui.lecturer.MarksEntryPanel;
import com.facultyams.ui.lecturer.ResultsPanel;
import com.facultyams.ui.lecturer.StudentResultsPanel;
import com.facultyams.ui.lecturer.StudentsPanel;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Offline checks for the Lecturer module. They need NO database: the DAOs are replaced by
 * in-memory fakes, so the real service rules (authorization, validation, locks, calculations)
 * run exactly as in the application. SQL text itself is NOT exercised here.
 *
 * Run from the project root:  java -cp target/classes com.facultyams.LecturerChecks
 */
public final class LecturerChecks {

    private static int passed;
    private static int failed;

    private LecturerChecks() {
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    private interface Thrower {
        void run() throws Exception;
    }

    private static boolean throwsA(Class<? extends Throwable> type, Thrower t) {
        try {
            t.run();
            return false;
        } catch (Throwable e) {
            return type.isInstance(e);
        }
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 0.0051;
    }

    // ================================================================== in-memory database

    /** Everything the fake DAOs share, like the tables of one database. */
    private static final class World {
        final Map<Integer, Course> courses = new LinkedHashMap<>();
        final Map<Integer, List<StudentRow>> studentsOfCourse = new HashMap<>();
        final Map<Integer, EvaluationComponent> components = new LinkedHashMap<>();
        final Map<String, Double> componentMarks = new LinkedHashMap<>();        // "student:component"
        final Map<String, Marks> results = new LinkedHashMap<>();                // "student:course"
        final Map<Integer, Map<Integer, AttendanceAgg>> attendance = new HashMap<>(); // course -> student
        final Map<Integer, Integer> attendanceComponents = new HashMap<>();
        final List<CourseMaterial> materials = new ArrayList<>();
        int nextComponentId = 100;
        int resultWrites;
        int saveCalls;

        int lecturerOf(int courseId) {
            Course c = courses.get(courseId);
            return c == null || c.getLecturerId() == null ? -1 : c.getLecturerId();
        }

        StudentRow student(int id) {
            for (List<StudentRow> list : studentsOfCourse.values()) {
                for (StudentRow s : list) {
                    if (s.studentId() == id) {
                        return s;
                    }
                }
            }
            return null;
        }

        List<StudentRow> all() {
            List<StudentRow> out = new ArrayList<>();
            for (List<StudentRow> list : studentsOfCourse.values()) {
                for (StudentRow s : list) {
                    if (out.stream().noneMatch(o -> o.studentId() == s.studentId())) {
                        out.add(s);
                    }
                }
            }
            return out;
        }
    }

    private static final class FakeCourseDAO extends CourseDAO {
        private final World w;

        FakeCourseDAO(World w) {
            this.w = w;
        }

        @Override
        public Course findById(int id) {
            return w.courses.get(id);
        }

        @Override
        public List<Course> findByLecturer(int lecturerId) {
            List<Course> list = new ArrayList<>();
            for (Course c : w.courses.values()) {
                if (c.getLecturerId() != null && c.getLecturerId() == lecturerId) {
                    list.add(c);
                }
            }
            return list;
        }
    }

    private static final class FakeAccessDAO extends LecturerAccessDAO {
        private final World w;

        FakeAccessDAO(World w) {
            this.w = w;
        }

        @Override
        public boolean ownsCourse(int lecturerId, int courseId) {
            return w.lecturerOf(courseId) == lecturerId;
        }

        @Override
        public List<StudentRow> findStudentsOfCourse(int lecturerId, int courseId) {
            return ownsCourse(lecturerId, courseId) ? w.studentsOfCourse.getOrDefault(courseId, List.of()) : List.of();
        }

        @Override
        public boolean isStudentInCourse(int lecturerId, int courseId, int studentId) {
            return findStudentsOfCourse(lecturerId, courseId).stream().anyMatch(s -> s.studentId() == studentId);
        }

        @Override
        public boolean isStudentAuthorized(int lecturerId, int studentId) {
            for (Course c : w.courses.values()) {
                if (isStudentInCourse(lecturerId, c.getCourseId(), studentId)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<StudentRow> searchStudents(int lecturerId, String text) {
            List<StudentRow> out = new ArrayList<>();
            for (StudentRow s : w.all()) {
                if (isStudentAuthorized(lecturerId, s.studentId())
                        && (s.registrationNo().contains(text.trim()) || s.fullName().contains(text.trim()))) {
                    out.add(s);
                }
            }
            return out;
        }

        @Override
        public int countStudents(int lecturerId) {
            return (int) w.all().stream().filter(s -> isStudentAuthorized(lecturerId, s.studentId())).count();
        }

        @Override
        public int countAttendanceComponents(int courseId) {
            return w.attendanceComponents.getOrDefault(courseId, 0);
        }

        @Override
        public List<AttendanceAgg> attendanceByStudent(int courseId) {
            return new ArrayList<>(w.attendance.getOrDefault(courseId, Map.of()).values());
        }

        @Override
        public List<AttendanceRecord> attendanceRecords(int courseId, int studentId) {
            return List.of(new AttendanceRecord("THEORY", 1, LocalDate.of(2026, 1, 5), "present", null));
        }

        @Override
        public List<MedicalRow> medicalRows(int lecturerId, int courseId) {
            if (!ownsCourse(lecturerId, courseId)) {
                return List.of();
            }
            return List.of(
                    new MedicalRow(1, "TG/2023/1001", "Kasun", "THEORY", 3, LocalDate.of(2026, 1, 6),
                            LocalDate.of(2026, 1, 7), "approved", "Mr. Kumara"),
                    new MedicalRow(2, "TG/2023/1002", "Nethmi", "THEORY", 4, LocalDate.of(2026, 1, 8),
                            LocalDate.of(2026, 1, 9), "pending", "Mr. Kumara"));
        }

        @Override
        public int countPendingMedical(int lecturerId) {
            return 1;
        }
    }

    private static final class FakeComponentDAO extends EvaluationComponentDAO {
        private final World w;

        FakeComponentDAO(World w) {
            this.w = w;
        }

        @Override
        public List<EvaluationComponent> findByCourse(int courseId) {
            List<EvaluationComponent> list = new ArrayList<>();
            for (EvaluationComponent c : w.components.values()) {
                if (c.getCourseId() == courseId) {
                    list.add(c);
                }
            }
            return list;
        }

        @Override
        public EvaluationComponent findById(int id) {
            return w.components.get(id);
        }

        @Override
        public boolean nameExists(int courseId, String name, int excludeId) {
            return findByCourse(courseId).stream().anyMatch(c ->
                    c.getComponentName().equalsIgnoreCase(name) && c.getEvalComponentId() != excludeId);
        }

        @Override
        public int insert(EvaluationComponent c) {
            c.setEvalComponentId(w.nextComponentId++);
            w.components.put(c.getEvalComponentId(), c);
            return c.getEvalComponentId();
        }

        @Override
        public void update(EvaluationComponent c) {
            w.components.put(c.getEvalComponentId(), c);
        }

        @Override
        public void delete(int id) {
            w.components.remove(id);
        }

        @Override
        public int countMarksForComponent(int id) {
            return (int) w.componentMarks.keySet().stream().filter(k -> k.endsWith(":" + id)).count();
        }

        @Override
        public int countMarksForCourse(int courseId) {
            return (int) w.componentMarks.keySet().stream()
                    .filter(k -> w.components.get(Integer.parseInt(k.split(":")[1])).getCourseId() == courseId).count();
        }

        @Override
        public Map<Integer, Map<Integer, Double>> findMarksByCourse(int courseId) {
            Map<Integer, Map<Integer, Double>> out = new HashMap<>();
            w.componentMarks.forEach((k, v) -> {
                String[] p = k.split(":");
                if (w.components.get(Integer.parseInt(p[1])).getCourseId() == courseId) {
                    out.computeIfAbsent(Integer.parseInt(p[0]), x -> new LinkedHashMap<>()).put(Integer.parseInt(p[1]), v);
                }
            });
            return out;
        }

        @Override
        public Map<Integer, Double> findMarksOfStudent(int courseId, int studentId) {
            return findMarksByCourse(courseId).getOrDefault(studentId, new LinkedHashMap<>());
        }
    }

    private static final class FakeMarksDAO extends MarksDAO {
        private final World w;

        FakeMarksDAO(World w) {
            this.w = w;
        }

        @Override
        public Marks findResult(int studentId, int courseId) {
            return w.results.get(studentId + ":" + courseId);
        }

        @Override
        public List<ResultRow> findResultsByCourse(int courseId) {
            List<ResultRow> rows = new ArrayList<>();
            for (Map.Entry<String, Marks> e : w.results.entrySet()) {
                Marks m = e.getValue();
                if (m.getCourseId() == courseId) {
                    StudentRow s = w.student(m.getStudentId());
                    rows.add(new ResultRow(m.getStudentId(), s.registrationNo(), s.fullName(), m.getObtainMarks(),
                            m.getFinalMarks(), m.getOverallMark(), m.getGrade(), m.getGradePoints(),
                            m.isEligible(), m.isPublished()));
                }
            }
            return rows;
        }

        @Override
        public void saveStudentMarks(int courseId, int studentId, int enteredBy,
                                     Map<Integer, Double> componentMarks, Marks result) throws DatabaseException {
            w.saveCalls++;
            Marks existing = w.results.get(studentId + ":" + courseId);
            if (existing != null && existing.isPublished()) {
                throw new DatabaseException("locked");
            }
            componentMarks.forEach((cid, v) -> w.componentMarks.put(studentId + ":" + cid, v));
            if (result != null) {
                result.setStudentId(studentId);
                result.setCourseId(courseId);
                w.results.put(studentId + ":" + courseId, result);   // one row per student and course
                w.resultWrites++;
            }
        }

        @Override
        public int publish(int courseId) {
            int n = 0;
            for (Marks m : w.results.values()) {
                if (m.getCourseId() == courseId && !m.isPublished()) {
                    m.setPublished(true);
                    n++;
                }
            }
            return n;
        }

        @Override
        public int unpublish(int courseId) {
            int n = 0;
            for (Marks m : w.results.values()) {
                if (m.getCourseId() == courseId && m.isPublished()) {
                    m.setPublished(false);
                    n++;
                }
            }
            return n;
        }

        @Override
        public int countPublished(int courseId) {
            return (int) w.results.values().stream().filter(m -> m.getCourseId() == courseId && m.isPublished()).count();
        }

        @Override
        public int countResults(int courseId) {
            return (int) w.results.values().stream().filter(m -> m.getCourseId() == courseId).count();
        }

        @Override
        public List<StudentCourseResult> findStudentResults(int studentId, int lecturerId) {
            List<StudentCourseResult> out = new ArrayList<>();
            for (Marks m : w.results.values()) {
                if (m.getStudentId() == studentId) {
                    Course c = w.courses.get(m.getCourseId());
                    out.add(new StudentCourseResult(c.getCourseId(), c.getCourseCode(), c.getCourseName(),
                            c.getCredits(), c.getAcademicLevel(), c.getSemester(), m.getObtainMarks(),
                            m.getFinalMarks(), m.getOverallMark(), m.getGrade(), m.getGradePoints(),
                            m.isEligible(), m.isPublished(), c.getLecturerId() != null && c.getLecturerId() == lecturerId));
                }
            }
            return out;
        }
    }

    private static final class FakeMaterialDAO extends CourseMaterialDAO {
        private final World w;

        FakeMaterialDAO(World w) {
            this.w = w;
        }

        @Override
        public List<CourseMaterial> findByLecturer(int lecturerId) {
            List<CourseMaterial> out = new ArrayList<>();
            for (CourseMaterial m : w.materials) {
                if (w.lecturerOf(m.getCourseID()) == lecturerId) {
                    out.add(m);
                }
            }
            return out;
        }

        @Override
        public int findCourseId(int materialId) {
            for (CourseMaterial m : w.materials) {
                if (m.getMaterialID() == materialId) {
                    return m.getCourseID();
                }
            }
            return -1;
        }

        @Override
        public boolean addMaterial(CourseMaterial m) {
            w.materials.add(m);
            return true;
        }

        @Override
        public boolean updateMaterial(CourseMaterial m) {
            return true;
        }
    }

    private static final class FakeLecturerDAO extends LecturerDAO {
        boolean updated;

        @Override
        public Lecturer findByLecturerID(int id) {
            Lecturer l = new Lecturer(id, "lec" + id, "Lecturer " + id, "l" + id + "@fot.example.lk", "Lecturer");
            l.setPhone("0711234567");
            return l;
        }

        @Override
        public void updateProfile(int userID, String fullName, String email, String phone, String pic) {
            updated = true;
        }
    }

    // ================================================================== fixture

    private static final int L2 = 2;     // lecturer of course 1
    private static final int L3 = 3;     // lecturer of course 2
    private static final int C1 = 1;
    private static final int C2 = 2;
    private static final int S11 = 11;
    private static final int S12 = 12;
    private static final int S13 = 13;
    private static final int S14 = 14;

    private static World world() {
        World w = new World();
        Course c1 = new Course(C1, "ICT2113", "Data Structures", 3, 2, 1, Course.CourseType.THEORY_AND_PRACTICAL, 1);
        c1.setLecturerId(L2);
        Course c2 = new Course(C2, "ICT2122", "OOP", 2, 2, 1, Course.CourseType.THEORY, 1);
        c2.setLecturerId(L3);
        w.courses.put(C1, c1);
        w.courses.put(C2, c2);
        w.studentsOfCourse.put(C1, List.of(
                new StudentRow(S11, "TG/2023/1001", "Kasun", "B09", "REGULAR"),
                new StudentRow(S12, "TG/2023/1002", "Nethmi", "B09", "REGULAR"),
                new StudentRow(S13, "TG/2023/1003", "Isuru", "B09", "REGULAR")));
        w.studentsOfCourse.put(C2, List.of(new StudentRow(S14, "TG/2023/1004", "Sachini", "B09", "REGULAR")));
        // course 1 scheme: CA part 40 (Quiz 10, Mid 10, Assignment 10, Practical 10), final exam 60
        addComponent(w, 1, C1, "Quiz", EvaluationComponent.CA, 20, 10);
        addComponent(w, 2, C1, "Mid", EvaluationComponent.CA, 100, 10);
        addComponent(w, 3, C1, "Assignment", EvaluationComponent.ASSIGNMENT, 100, 10);
        addComponent(w, 4, C1, "Practical", EvaluationComponent.PRACTICAL, 100, 10);
        addComponent(w, 5, C1, "Final", EvaluationComponent.FINAL_EXAM, 100, 60);
        // course 2 scheme is deliberately incomplete (weights add up to 60)
        addComponent(w, 6, C2, "Test", EvaluationComponent.CA, 100, 20);
        addComponent(w, 7, C2, "Final", EvaluationComponent.FINAL_EXAM, 100, 40);
        w.attendanceComponents.put(C1, 2);                                   // theory + practical = 30 sessions
        w.attendance.put(C1, new HashMap<>());
        w.attendance.get(C1).put(S11, new AttendanceAgg(S11, "TG/2023/1001", "Kasun", 28, 2, 0));    // 93.33 %
        w.attendance.get(C1).put(S12, new AttendanceAgg(S12, "TG/2023/1002", "Nethmi", 20, 10, 3));  // 76.67 %
        return w;
    }

    private static void addComponent(World w, int id, int course, String name, String cat, double max, double weight) {
        w.components.put(id, new EvaluationComponent(id, course, name, cat, max, weight));
    }

    private static final class Svc {
        final World w = world();
        final LecturerAccessService access = new LecturerAccessService(new FakeAccessDAO(w), new FakeCourseDAO(w));
        final FakeComponentDAO components = new FakeComponentDAO(w);
        final FakeMarksDAO marksDao = new FakeMarksDAO(w);
        final EvaluationComponentService evaluation = new EvaluationComponentService(access, components, marksDao);
        final MarksService marks = new MarksService(access, components, marksDao);
        final GPAService gpa = new GPAService(access, marksDao);
        final ResultService results = new ResultService(access, components, marksDao, gpa);
        final LecturerMedicalService medical = new LecturerMedicalService(access, new FakeAccessDAO(w));
        final FakeLecturerDAO lecturerDao = new FakeLecturerDAO();
        final LecturerService lecturer = new LecturerService(lecturerDao, access, new FakeAccessDAO(w), components, marksDao);
        final CourseMaterialService materials = new CourseMaterialService(access, new FakeMaterialDAO(w));
    }

    private static void loginLecturer(int id) {
        Lecturer l = new Lecturer();
        l.setUserId(id);
        l.setUsername("lec" + id);
        l.setFullName("Lecturer " + id);
        l.setActive(true);
        Session.start(l);
    }

    private static Map<Integer, Double> marks(Object... pairs) {
        Map<Integer, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((Integer) pairs[i], ((Number) pairs[i + 1]).doubleValue());
        }
        return m;
    }

    // ================================================================== main

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        rulesChecks();
        authorizationChecks();
        marksEntryChecks();
        componentChecks();
        resultsAndPublishChecks();
        gpaChecks();
        privacyChecks();
        panelChecks();
        Session.end();
        System.out.println("\nRESULT: " + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ [1] pure rules

    private static void rulesChecks() throws Exception {
        System.out.println("[1] Grading scale, weighted marks, eligibility, GPA (AcademicRules)");
        check("grade boundaries: 85=A+, 84.99=A, 75=A, 74.99=A-, 70=A-, 69.99=B+",
                "A+".equals(AcademicRules.gradeFor(85)) && "A".equals(AcademicRules.gradeFor(84.99))
                        && "A".equals(AcademicRules.gradeFor(75)) && "A-".equals(AcademicRules.gradeFor(74.99))
                        && "A-".equals(AcademicRules.gradeFor(70)) && "B+".equals(AcademicRules.gradeFor(69.99)));
        check("grade boundaries: 40=C-, 39.99=D+, 30=D, 29.99=E, 0=E, 100=A+",
                "C-".equals(AcademicRules.gradeFor(40)) && "D+".equals(AcademicRules.gradeFor(39.99))
                        && "D".equals(AcademicRules.gradeFor(30)) && "E".equals(AcademicRules.gradeFor(29.99))
                        && "E".equals(AcademicRules.gradeFor(0)) && "A+".equals(AcademicRules.gradeFor(100)));
        check("grade points: A+ 4.0, A 4.0, A- 3.7, C- 1.7, E 0.0",
                AcademicRules.gradePointsFor(90) == 4.0 && AcademicRules.gradePointsFor(80) == 4.0
                        && AcademicRules.gradePointsFor(72) == 3.7 && AcademicRules.gradePointsFor(41) == 1.7
                        && AcademicRules.gradePointsFor(10) == 0.0);
        check("overall mark outside 0-100 or NaN is rejected",
                throwsA(IllegalArgumentException.class, () -> AcademicRules.gradeFor(-0.01))
                        && throwsA(IllegalArgumentException.class, () -> AcademicRules.gradeFor(100.01))
                        && throwsA(IllegalArgumentException.class, () -> AcademicRules.gradeFor(Double.NaN)));

        List<EvaluationComponent> scheme = new ArrayList<>(world().components.values().stream()
                .filter(c -> c.getCourseId() == C1).toList());
        check("scheme 10+10+10+10+60 is complete", AcademicRules.checkScheme(scheme).complete());
        check("scheme with weights 60 is incomplete and says so",
                !AcademicRules.checkScheme(new ArrayList<>(world().components.values().stream()
                        .filter(c -> c.getCourseId() == C2).toList())).complete());
        check("empty scheme is incomplete", !AcademicRules.checkScheme(List.of()).complete());
        check("scheme without a final exam is incomplete",
                !AcademicRules.checkScheme(List.of(new EvaluationComponent(1, 1, "T", "CA", 100, 100))).complete());
        check("0.1 + 0.2 style weights add up exactly (33.33+33.33+33.34 = 100)",
                AcademicRules.checkScheme(List.of(
                        new EvaluationComponent(1, 1, "A", "CA", 100, 33.33),
                        new EvaluationComponent(2, 1, "B", "ASSIGNMENT", 100, 33.33),
                        new EvaluationComponent(3, 1, "F", "FINAL_EXAM", 100, 33.34))).complete());

        Map<Integer, Double> m11 = marks(1, 18, 2, 70, 3, 80, 4, 60, 5, 75);
        AcademicRules.Result r = AcademicRules.computeResult(scheme, m11, new AttendanceInfo(28, 2, 0, 30));
        check("CA % = (9+7+8+6)/40 = 75.00, final % = 75.00, overall = 75.00 -> A, 4.0",
                r != null && near(r.caPercent(), 75) && near(r.finalPercent(), 75) && near(r.overall(), 75)
                        && "A".equals(r.grade()) && r.gradePoints() == 4.0);
        check("attendance 28/30 = 93.33 % and CA 75 % -> eligible", r != null && r.eligibility().eligible());
        check("result is null while any component mark is missing",
                AcademicRules.computeResult(scheme, marks(1, 18, 2, 70, 3, 80, 4, 60), AttendanceInfo.none()) == null);
        check("a quiz marked out of 20 is converted correctly (20/20 -> full weight)",
                near(AcademicRules.percentOf(List.of(scheme.get(0)), marks(1, 20)), 100));
        check("zero marks everywhere -> overall 0.00, grade E, 0.0 points",
                near(AcademicRules.computeResult(scheme, marks(1, 0, 2, 0, 3, 0, 4, 0, 5, 0), AttendanceInfo.none()).overall(), 0));
        check("full marks everywhere -> overall 100.00, A+",
                "A+".equals(AcademicRules.computeResult(scheme, marks(1, 20, 2, 100, 3, 100, 4, 100, 5, 100), AttendanceInfo.none()).grade()));
        check("rounding: overall 74.995 rounds half-up to 75.00 -> grade A (not A-)",
                "A".equals(AcademicRules.gradeFor(AcademicRules.percentOf(
                        List.of(new EvaluationComponent(1, 1, "F", "FINAL_EXAM", 10000, 100)),
                        marks(1, 7499.5)))));

        EvaluationComponent quiz = scheme.get(0);
        check("mark validation: negative, above max, NaN, infinity rejected; max itself accepted",
                throwsA(ValidationException.class, () -> AcademicRules.requireValidMark(quiz, -1))
                        && throwsA(ValidationException.class, () -> AcademicRules.requireValidMark(quiz, 20.01))
                        && throwsA(ValidationException.class, () -> AcademicRules.requireValidMark(quiz, Double.NaN))
                        && throwsA(ValidationException.class, () -> AcademicRules.requireValidMark(quiz, Double.POSITIVE_INFINITY))
                        && AcademicRules.requireValidMark(quiz, 20) == 20);

        check("eligibility: CA 40.00 passes, 39.99 fails",
                AcademicRules.eligibility(40.0, new AttendanceInfo(15, 0, 0, 15)).eligible()
                        && AcademicRules.eligibility(39.99, new AttendanceInfo(15, 0, 0, 15)).status() == AcademicRules.Status.NOT_ELIGIBLE);
        check("eligibility: attendance 12/15 = 80 % passes, 11/15 = 73.33 % fails",
                AcademicRules.eligibility(80.0, new AttendanceInfo(12, 3, 0, 15)).eligible()
                        && AcademicRules.eligibility(80.0, new AttendanceInfo(11, 4, 0, 15)).status() == AcademicRules.Status.NOT_ELIGIBLE);
        check("eligibility: approved medical absences count as attended (11 + 1 medical = 80 %)",
                AcademicRules.eligibility(80.0, new AttendanceInfo(11, 4, 1, 15)).eligible());
        check("eligibility: attendance never exceeds 100 %", new AttendanceInfo(15, 0, 3, 15).percent() == 100.0);
        check("eligibility: no attendance rows = 'Pending / not recorded', never 0 %",
                AcademicRules.eligibility(80.0, AttendanceInfo.none()).status() == AcademicRules.Status.PENDING
                        && !AttendanceInfo.none().recorded());
        check("eligibility: CA marks incomplete + attendance fine = Pending",
                AcademicRules.eligibility(null, new AttendanceInfo(15, 0, 0, 15)).status() == AcademicRules.Status.PENDING);
        check("eligibility: failed attendance is final even when CA is incomplete",
                AcademicRules.eligibility(null, new AttendanceInfo(5, 10, 0, 15)).status() == AcademicRules.Status.NOT_ELIGIBLE);
        AttendanceService shared = new AttendanceService();
        check("attendance rule here equals the shared AttendanceService rule (79.99 no, 80 yes)",
                !shared.isEligible(79.99) && shared.isEligible(80) && AcademicRules.ATTENDANCE_MIN_PERCENT == 80);
    }

    // ------------------------------------------------------------------ [2] authorization

    private static void authorizationChecks() throws Exception {
        System.out.println("[2] Authorization (role, course ownership, student membership)");
        Svc s = new Svc();
        Session.end();
        check("logged out: assigned courses denied", throwsA(AuthorizationException.class, s.access::getAssignedCourses));
        check("logged out: marks entry sheet denied", throwsA(AuthorizationException.class, () -> s.marks.loadEntrySheet(C1)));
        User student = new Student();
        student.setUserId(S11);
        student.setActive(true);
        Session.start(student);
        check("student session: cannot load a lecturer's marks sheet", throwsA(AuthorizationException.class, () -> s.marks.loadEntrySheet(C1)));
        check("student session: cannot save marks", throwsA(AuthorizationException.class,
                () -> s.marks.saveStudentMarks(C1, S11, marks(1, 20))));
        check("student session: cannot publish", throwsA(AuthorizationException.class, () -> s.results.publish(C1)));

        loginLecturer(L2);
        check("lecturer sees only own courses (lecturer 2 -> 1 course, ICT2113)",
                s.access.getAssignedCourses().size() == 1 && s.access.getAssignedCourses().get(0).getCourseId() == C1);
        check("lecturer 2: own course sheet loads", s.marks.loadEntrySheet(C1).rows().size() == 3);

        String denied = "lecturer 2 on lecturer 3's course: ";
        check(denied + "scheme", throwsA(AuthorizationException.class, () -> s.evaluation.getScheme(C2)));
        check(denied + "add component", throwsA(AuthorizationException.class,
                () -> s.evaluation.addComponent(C2, "Quiz", "CA", 10, 5)));
        check(denied + "edit component", throwsA(AuthorizationException.class,
                () -> s.evaluation.updateComponent(6, "X", "CA", 100, 20)));
        check(denied + "delete component", throwsA(AuthorizationException.class, () -> s.evaluation.deleteComponent(6)));
        check(denied + "marks sheet", throwsA(AuthorizationException.class, () -> s.marks.loadEntrySheet(C2)));
        check(denied + "save marks", throwsA(AuthorizationException.class,
                () -> s.marks.saveStudentMarks(C2, S14, marks(6, 50))));
        check(denied + "eligibility", throwsA(AuthorizationException.class, () -> s.results.getEligibility(C2)));
        check(denied + "final results", throwsA(AuthorizationException.class, () -> s.results.getCourseResults(C2)));
        check(denied + "publish", throwsA(AuthorizationException.class, () -> s.results.publish(C2)));
        check(denied + "unpublish", throwsA(AuthorizationException.class, () -> s.results.unpublish(C2)));
        check(denied + "recalculate", throwsA(AuthorizationException.class, () -> s.results.recalculate(C2)));
        check(denied + "attendance", throwsA(AuthorizationException.class, () -> s.lecturer.getAttendance(C2)));
        check(denied + "medical records", throwsA(AuthorizationException.class, () -> s.medical.getMedicalRecords(C2)));
        check("a student of another lecturer's course cannot get marks in my course",
                throwsA(AuthorizationException.class, () -> s.marks.saveStudentMarks(C1, S14, marks(1, 10))));
        check("unknown course id gives the same denial as someone else's course",
                throwsA(AuthorizationException.class, () -> s.marks.loadEntrySheet(999)));
        check("student search only finds students of own courses (TG/2023/1004 hidden)",
                s.results.searchStudents("TG/2023/1004").isEmpty() && s.results.searchStudents("TG/2023/1001").size() == 1);
        check("blank student search is a validation error", throwsA(ValidationException.class, () -> s.results.searchStudents("  ")));
        check("viewing results of a student outside my courses is denied", throwsA(AuthorizationException.class,
                () -> s.results.getStudentResults(new StudentRow(S14, "TG/2023/1004", "Sachini", "B09", "REGULAR"))));
        check("GPA of a student outside my courses is denied", throwsA(AuthorizationException.class, () -> s.gpa.calculate(S14)));
        check("profile: lecturer 2 cannot edit lecturer 3's profile", throwsA(AuthorizationException.class,
                () -> s.lecturer.updateProfile(L3, "Name", "a@b.lk", "0711234567", null)));
        check("profile: own profile comes from the session (lecturer 2), not a fixed id",
                s.lecturer.getCurrentLecturer().getLecturerID() == L2);
        loginLecturer(L3);
        check("profile: after switching user the profile is lecturer 3", s.lecturer.getCurrentLecturer().getLecturerID() == L3);
        loginLecturer(L2);
        check("profile: invalid email / phone rejected", throwsA(ValidationException.class,
                () -> s.lecturer.updateProfile(L2, "Name", "not-an-email", "0711234567", null))
                && throwsA(ValidationException.class, () -> s.lecturer.updateProfile(L2, "Name", "a@b.lk", "abc", null)));
        s.lecturer.updateProfile(L2, "Name", "a@b.lk", "0711234567", null);
        check("profile: valid update reaches the DAO", s.lecturerDao.updated);

        s.w.materials.add(new CourseMaterial(7, C2, "Other lecturer's slides", "", ""));
        check("materials: only own courses listed", s.materials.getMyMaterials().isEmpty());
        check("materials: cannot add to another lecturer's course", throwsA(AuthorizationException.class,
                () -> s.materials.addMaterial(new CourseMaterial(0, C2, "x", "", ""))));
        check("materials: cannot edit another lecturer's material", throwsA(AuthorizationException.class,
                () -> s.materials.updateMaterial(new CourseMaterial(7, C1, "x", "", ""))));
        s.materials.addMaterial(new CourseMaterial(0, C1, "Week 1 slides", "", ""));
        check("materials: adding to own course works", s.materials.getMyMaterials().size() == 1);
        check("materials: blank description rejected", throwsA(ValidationException.class,
                () -> s.materials.addMaterial(new CourseMaterial(0, C1, "  ", "", ""))));
    }

    // ------------------------------------------------------------------ [3] marks entry

    private static void marksEntryChecks() throws Exception {
        System.out.println("[3] Marks entry: validation, persistence, duplicates, locking");
        Svc s = new Svc();
        loginLecturer(L2);
        check("invalid marks are rejected and nothing is saved (negative / over max / NaN)",
                throwsA(ValidationException.class, () -> s.marks.saveStudentMarks(C1, S11, marks(1, -1)))
                        && throwsA(ValidationException.class, () -> s.marks.saveStudentMarks(C1, S11, marks(1, 21)))
                        && throwsA(ValidationException.class, () -> s.marks.saveStudentMarks(C1, S11, marks(1, Double.NaN)))
                        && s.w.saveCalls == 0 && s.w.componentMarks.isEmpty());
        check("one bad mark rejects the whole save (valid quiz + invalid mid exam -> nothing stored)",
                throwsA(ValidationException.class, () -> s.marks.saveStudentMarks(C1, S11, marks(1, 10, 2, 101)))
                        && s.w.componentMarks.isEmpty());
        check("a mark for a component of ANOTHER course is rejected",
                throwsA(ValidationException.class, () -> s.marks.saveStudentMarks(C1, S11, marks(6, 10))));
        check("empty entry rejected", throwsA(ValidationException.class, () -> s.marks.saveStudentMarks(C1, S11, Map.of())));
        check("scheme with weights != 100 blocks marks entry",
                thrownMessage(() -> {
                    loginLecturer(L3);
                    s.marks.saveStudentMarks(C2, S14, marks(6, 50));
                }).contains("incomplete"));
        loginLecturer(L2);

        AcademicRules.Result partial = s.marks.saveStudentMarks(C1, S11, marks(1, 18, 2, 70));
        check("partial entry stores the marks but calculates no result",
                partial == null && s.w.componentMarks.size() == 2 && s.w.results.isEmpty());
        AcademicRules.Result done = s.marks.saveStudentMarks(C1, S11, marks(3, 80, 4, 60, 5, 75));
        check("completing the last marks calculates the result (75.00, A, 4.0, eligible)",
                done != null && near(done.overall(), 75) && "A".equals(done.grade()) && done.eligibility().eligible()
                        && s.w.results.size() == 1);
        Marks stored = s.w.results.get(S11 + ":" + C1);
        check("stored row carries CA %, final %, overall, grade, grade points, eligibility",
                stored != null && near(stored.getObtainMarks(), 75) && near(stored.getFinalMarks(), 75)
                        && near(stored.getOverallMark(), 75) && "A".equals(stored.getGrade())
                        && stored.getGradePoints() == 4.0 && stored.isEligible() && !stored.isPublished());

        s.marks.saveStudentMarks(C1, S11, marks(5, 85));
        check("updating a mark recalculates (final 85 -> overall 81.00) without a duplicate row",
                s.w.results.size() == 1 && near(s.w.results.get(S11 + ":" + C1).getOverallMark(), 81)
                        && s.w.componentMarks.size() == 5);
        s.marks.saveStudentMarks(C1, S11, marks(5, 85));
        check("saving the same value twice creates no duplicates", s.w.componentMarks.size() == 5 && s.w.results.size() == 1);

        s.marks.saveStudentMarks(C1, S12, marks(1, 4, 2, 30, 3, 40, 4, 50, 5, 50));
        Marks m12 = s.w.results.get(S12 + ":" + C1);
        check("CA 35 % and attendance 76.67 % -> stored as NOT eligible, overall 44.00 -> C-",
                m12 != null && !m12.isEligible() && near(m12.getOverallMark(), 44) && "C-".equals(m12.getGrade())
                        && m12.getGradePoints() == 1.7);
        s.marks.saveStudentMarks(C1, S13, marks(1, 20, 2, 100, 3, 100, 4, 100, 5, 100));
        check("student with no attendance recorded -> result stored but eligibility not confirmed",
                !s.w.results.get(S13 + ":" + C1).isEligible()
                        && near(s.w.results.get(S13 + ":" + C1).getOverallMark(), 100));

        MarksService.EntrySheet sheet = s.marks.loadEntrySheet(C1);
        check("entry sheet shows 3 students, 5 components and the saved overall marks",
                sheet.rows().size() == 3 && sheet.components().size() == 5 && sheet.scheme().complete()
                        && sheet.rows().get(0).hasResult());

        s.results.publish(C1);
        check("published result is locked: editing marks is rejected",
                throwsA(ValidationException.class, () -> s.marks.saveStudentMarks(C1, S11, marks(1, 19))));
        s.results.unpublish(C1);
        s.marks.saveStudentMarks(C1, S11, marks(1, 19));
        check("after unpublishing, marks can be edited again", s.w.componentMarks.get(S11 + ":1") == 19.0);
    }

    private static String thrownMessage(Thrower t) {
        try {
            t.run();
            return "";
        } catch (Exception e) {
            return e.getMessage() == null ? "" : e.getMessage();
        }
    }

    // ------------------------------------------------------------------ [4] components

    private static void componentChecks() throws Exception {
        System.out.println("[4] Evaluation components: weightings, duplicates, locks");
        Svc s = new Svc();
        loginLecturer(L3);                                   // course 2: Test 20 + Final 40 = 60 (theory only)
        check("weight total over 100 is rejected (60 + 50)", thrownMessage(
                () -> s.evaluation.addComponent(C2, "Assignment", "ASSIGNMENT", 100, 50)).contains("cannot exceed 100"));
        check("duplicate name rejected (case-insensitive)", throwsA(ValidationException.class,
                () -> s.evaluation.addComponent(C2, "test", "CA", 100, 10)));
        check("practical component rejected on a theory-only course", throwsA(ValidationException.class,
                () -> s.evaluation.addComponent(C2, "Lab", "PRACTICAL", 100, 10)));
        check("invalid values rejected: blank name, bad category, max 0, weight 0 / 101 / NaN", throwsA(ValidationException.class,
                () -> s.evaluation.addComponent(C2, " ", "CA", 100, 10))
                && throwsA(ValidationException.class, () -> s.evaluation.addComponent(C2, "A", "QUIZ", 100, 10))
                && throwsA(ValidationException.class, () -> s.evaluation.addComponent(C2, "B", "CA", 0, 10))
                && throwsA(ValidationException.class, () -> s.evaluation.addComponent(C2, "C", "CA", 100, 0))
                && throwsA(ValidationException.class, () -> s.evaluation.addComponent(C2, "D", "CA", 100, 101))
                && throwsA(ValidationException.class, () -> s.evaluation.addComponent(C2, "E", "CA", 100, Double.NaN)));
        s.evaluation.addComponent(C2, "Assignment", "ASSIGNMENT", 100, 40);
        check("adding the missing 40 % completes the scheme at exactly 100",
                s.evaluation.getScheme(C2).check().complete() && s.evaluation.getScheme(C2).check().totalWeight() == 100.0);
        check("a component cannot be added beyond 100 %", throwsA(ValidationException.class,
                () -> s.evaluation.addComponent(C2, "Extra", "CA", 100, 0.01)));
        s.evaluation.updateComponent(6, "Test", "CA", 100, 20);
        check("editing without marks entered is allowed", s.evaluation.getScheme(C2).components().size() == 3);

        s.marks.saveStudentMarks(C2, S14, marks(6, 50));
        check("after marks exist: adding a component is blocked", throwsA(ValidationException.class,
                () -> s.evaluation.addComponent(C2, "Late", "CA", 100, 0.01)));
        check("after marks exist: deleting a component is blocked", throwsA(ValidationException.class,
                () -> s.evaluation.deleteComponent(7)));
        check("after marks exist: changing weight / max / category of a marked component is blocked",
                throwsA(ValidationException.class, () -> s.evaluation.updateComponent(6, "Test", "CA", 100, 25))
                        && throwsA(ValidationException.class, () -> s.evaluation.updateComponent(6, "Test", "CA", 50, 20))
                        && throwsA(ValidationException.class, () -> s.evaluation.updateComponent(6, "Test", "ASSIGNMENT", 100, 20)));
        s.evaluation.updateComponent(6, "Class Test", "CA", 100, 20);
        check("after marks exist: renaming a marked component is still allowed",
                "Class Test".equals(s.w.components.get(6).getComponentName()));

        s.marks.saveStudentMarks(C2, S14, marks(7, 60, 100, 70));
        s.results.publish(C2);
        check("published course: scheme is fully locked (even renaming)", throwsA(ValidationException.class,
                () -> s.evaluation.updateComponent(6, "Renamed", "CA", 100, 20)));
    }

    // ------------------------------------------------------------------ [5] results, publish

    private static void resultsAndPublishChecks() throws Exception {
        System.out.println("[5] Eligibility sheet, final results, publish / recalculate");
        Svc s = new Svc();
        loginLecturer(L2);
        s.marks.saveStudentMarks(C1, S11, marks(1, 18, 2, 70, 3, 80, 4, 60, 5, 75));
        s.marks.saveStudentMarks(C1, S12, marks(1, 4, 2, 30, 3, 40, 4, 50, 5, 50));
        s.marks.saveStudentMarks(C1, S13, marks(1, 10, 2, 50));   // incomplete

        ResultService.EligibilitySheet elig = s.results.getEligibility(C1);
        Map<Integer, AcademicRules.Status> status = new HashMap<>();
        elig.rows().forEach(r -> status.put(r.student().studentId(), r.eligibility().status()));
        check("eligibility sheet: S11 eligible, S12 not eligible (CA 35 %, att 76.67 %), S13 pending",
                status.get(S11) == AcademicRules.Status.ELIGIBLE && status.get(S12) == AcademicRules.Status.NOT_ELIGIBLE
                        && status.get(S13) == AcademicRules.Status.PENDING);
        String reason12 = elig.rows().stream().filter(r -> r.student().studentId() == S12).findFirst().get().eligibility().reason();
        check("not-eligible reason names both failed requirements", reason12.contains("CA") && reason12.contains("ttendance"));

        ResultService.ResultSheet sheet = s.results.getCourseResults(C1);
        check("results sheet: 2 drafts, 1 incomplete, 0 published", sheet.drafts() == 2 && sheet.incomplete() == 1 && sheet.published() == 0);
        ResultService.PublishOutcome preview = s.results.previewPublish(C1);
        check("publish preview: 2 results, 1 skipped, 1 without confirmed eligibility",
                preview.toPublish() == 2 && preview.incomplete() == 1 && preview.notConfirmedEligible() == 1);
        check("preview changes nothing", s.results.getCourseResults(C1).published() == 0);
        s.results.publish(C1);
        check("publish publishes only the calculated results", s.results.getCourseResults(C1).published() == 2);
        check("publishing again with nothing new is a validation error",
                throwsA(ValidationException.class, () -> s.results.publish(C1)));
        check("recalculate never touches published results", s.results.recalculate(C1) == 0);
        s.results.unpublish(C1);
        s.w.attendance.get(C1).put(S12, new AttendanceAgg(S12, "TG/2023/1002", "Nethmi", 29, 1, 0));
        check("recalculate after attendance changed updates the stored results (2 results)", s.results.recalculate(C1) == 2);
        check("... and S12 is still not eligible because CA is only 35 %", !s.w.results.get(S12 + ":" + C1).isEligible());
    }

    // ------------------------------------------------------------------ [6] GPA

    private static void gpaChecks() throws Exception {
        System.out.println("[6] SGPA / CGPA");
        check("GPA = sum(points x credits) / sum(credits): (4.0x3 + 3.0x2 + 2.0x2)/7 = 3.14",
                near(AcademicRules.gpa(List.of(new AcademicRules.CreditResult(3, 4.0),
                        new AcademicRules.CreditResult(2, 3.0), new AcademicRules.CreditResult(2, 2.0))), 3.14));
        check("a failed 3-credit course (0.0) lowers GPA: (4.0x3 + 0.0x3)/6 = 2.00",
                near(AcademicRules.gpa(List.of(new AcademicRules.CreditResult(3, 4.0),
                        new AcademicRules.CreditResult(3, 0.0))), 2.0));
        check("GPA with no credits is null (not 0.00)", AcademicRules.gpa(List.of()) == null);
        check("GPA is rounded to 2 decimals: (3.7 + 3.3 + 3.0)/3 = 3.3333 -> 3.33",
                near(AcademicRules.gpa(List.of(new AcademicRules.CreditResult(1, 3.7),
                        new AcademicRules.CreditResult(1, 3.3), new AcademicRules.CreditResult(1, 3.0))), 3.33));

        List<MarksDAO.StudentCourseResult> rows = List.of(
                res(1, 3, 1, 1, 4.0, true), res(2, 2, 1, 1, 3.0, true),     // Level 1 Semester 1
                res(3, 3, 2, 1, 2.0, true), res(4, 2, 2, 1, 1.0, true),     // Level 2 Semester 1
                res(5, 4, 2, 1, 4.0, false));                                 // draft, must not count
        GPAService.GpaSummary g = GPAService.summarise(rows);
        check("SGPA is per level AND semester: L1S1 = 3.60, L2S1 = 1.60 (not merged into one 'semester 1')",
                g.semesters().size() == 2 && near(g.semesters().get(0).sgpa(), 3.6) && near(g.semesters().get(1).sgpa(), 1.6)
                        && g.semesters().get(0).level() == 1 && g.semesters().get(1).level() == 2);
        check("CGPA over all published results: (12+6+6+2)/10 = 2.60", near(g.cgpa(), 2.6) && g.creditsCounted() == 10);
        check("draft results are excluded and counted separately", g.draftResults() == 1);
        check("no published results -> CGPA null", GPAService.summarise(List.of(res(9, 3, 1, 1, 4.0, false))).cgpa() == null);
        check("results without a calculated grade are ignored",
                GPAService.summarise(List.of(new MarksDAO.StudentCourseResult(1, "X", "X", 3, 1, 1, 0, 0,
                        null, null, null, false, true, true))).cgpa() == null);

        Svc s = new Svc();
        loginLecturer(L2);
        s.marks.saveStudentMarks(C1, S11, marks(1, 18, 2, 70, 3, 80, 4, 60, 5, 75));
        check("lecturer GPA call for own student: draft not counted yet", s.gpa.calculate(S11).cgpa() == null
                && s.gpa.calculate(S11).draftResults() == 1);
        s.results.publish(C1);
        check("after publishing, CGPA = 4.00 for one 3-credit A", near(s.gpa.calculate(S11).cgpa(), 4.0));
        ResultService.StudentResults sr = s.results.getStudentResults(new StudentRow(S11, "TG/2023/1001", "Kasun", "B09", "REGULAR"));
        check("student results screen: course detail limited to my courses, GPA computed",
                sr.myCourses().size() == 1 && near(sr.gpa().cgpa(), 4.0));
    }

    private static MarksDAO.StudentCourseResult res(int id, int credits, int level, int semester, double gp, boolean published) {
        return new MarksDAO.StudentCourseResult(id, "C" + id, "Course " + id, credits, level, semester,
                50, 50, 50.0, "X", gp, true, published, true);
    }

    // ------------------------------------------------------------------ [7] privacy / SQL scoping

    private static void privacyChecks() throws Exception {
        System.out.println("[7] Medical privacy and query scoping");
        boolean hasReasonField = false;
        for (var rc : MedicalRow.class.getRecordComponents()) {
            hasReasonField |= rc.getName().toLowerCase().contains("reason");
        }
        check("MedicalRow (what a lecturer receives) has no reason / diagnosis field", !hasReasonField);
        String sql = (String) staticField(LecturerAccessDAO.class, "SQL_MEDICAL_ROWS");
        check("the lecturer medical query does not select the reason column", !sql.toLowerCase().contains("reason"));
        check("the lecturer medical query is restricted to the lecturer's courses",
                sql.contains("c.lecturer_id = ?"));
        check("student list / search / attendance queries are restricted by lecturer_id",
                ((String) staticField(LecturerAccessDAO.class, "SQL_STUDENTS_OF_COURSE")).contains("lecturer_id = ?")
                        && ((String) staticField(LecturerAccessDAO.class, "SQL_SEARCH_STUDENTS")).contains("lecturer_id = ?"));
        boolean upperCaseTable = false;
        int queries = 0;
        for (Class<?> c : new Class<?>[]{LecturerAccessDAO.class, MarksDAO.class, EvaluationComponentDAO.class, CourseMaterialDAO.class}) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getName().startsWith("SQL_")) {
                    f.setAccessible(true);
                    String q = (String) f.get(null);
                    queries++;
                    upperCaseTable |= q.contains("MEDICAL") || q.contains("MARKS ") || q.contains("COURSES ");
                }
            }
        }
        check("checked " + queries + " SQL statements: none uses an upper-case table name (MySQL on Linux is case-sensitive)",
                queries > 20 && !upperCaseTable);

        Session.end();
        Method guard = Class.forName("com.facultyams.ui.LecturerDashboard")
                .getDeclaredMethod("requireLecturerSession", User.class);
        guard.setAccessible(true);
        check("LecturerDashboard refuses null / not-logged-in users", invokeThrows(guard, (User) null)
                && invokeThrows(guard, lecturerUser(5)));
        User real = lecturerUser(5);
        Session.start(real);
        check("LecturerDashboard accepts the logged-in lecturer", !invokeThrows(guard, real));
        User student = new Student();
        student.setUserId(9);
        Session.start(student);
        check("LecturerDashboard refuses a student session", invokeThrows(guard, student));
    }

    private static User lecturerUser(int id) {
        Lecturer l = new Lecturer();
        l.setUserId(id);
        return l;
    }

    private static boolean invokeThrows(Method m, User arg) {
        try {
            m.invoke(null, arg);
            return false;
        } catch (Exception e) {
            return e.getCause() instanceof IllegalStateException;
        }
    }

    private static Object staticField(Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    // ------------------------------------------------------------------ [8] screens (headless)

    private static void panelChecks() throws Exception {
        System.out.println("[8] Screens render real data (headless, fake database)");
        Field sync = Class.forName("com.facultyams.ui.lecturer.LecturerUi").getDeclaredField("synchronous");
        sync.setAccessible(true);
        sync.set(null, true);
        Svc s = new Svc();
        loginLecturer(L2);
        s.marks.saveStudentMarks(C1, S11, marks(1, 18, 2, 70, 3, 80, 4, 60, 5, 75));
        s.marks.saveStudentMarks(C1, S12, marks(1, 4, 2, 30, 3, 40, 4, 50, 5, 50));

        DashboardHomePanel home = new DashboardHomePanel(s.lecturer, x -> { });
        home.refresh();
        check("dashboard: welcome shows the logged-in lecturer and a course row",
                text(home).contains("Welcome, Lecturer 2") && rows(home) == 1);

        EvaluationComponentsPanel ec = new EvaluationComponentsPanel(s.access, s.evaluation);
        ec.refresh();
        check("evaluation components: 5 rows, 'scheme complete' summary", rows(ec) == 5 && text(ec).contains("Scheme complete"));

        MarksEntryPanel me = new MarksEntryPanel(s.access, s.marks);
        me.refresh();
        JTable entry = table(me);
        check("marks entry: 3 students, 5 editable component columns + results",
                entry != null && entry.getRowCount() == 3 && entry.getColumnCount() == 2 + 5 + 3
                        && entry.getModel().isCellEditable(0, 2) && !entry.getModel().isCellEditable(0, 0));
        check("marks entry: overall shown from the database (75.00 / A)",
                "75.00".equals(entry.getModel().getValueAt(0, 7)) && "A".equals(entry.getModel().getValueAt(0, 8)));

        EligibilityPanel el = new EligibilityPanel(s.access, s.results);
        el.refresh();
        check("eligibility screen: 3 rows with Eligible / Not eligible / Pending",
                rows(el) == 3 && text(el).contains("Eligible") && text(el).contains("Not eligible"));
        check("eligibility screen: student without attendance shows 'Not recorded'", cellsContain(table(el), "Not recorded"));

        ResultsPanel rp = new ResultsPanel(s.access, s.results);
        rp.refresh();
        check("final results screen: grade, points, draft status and counts",
                rows(rp) == 3 && cellsContain(table(rp), "A") && cellsContain(table(rp), "Draft")
                        && text(rp).contains("2 draft, 1 incomplete"));

        StudentResultsPanel sp = new StudentResultsPanel(s.results);
        check("student results screen builds", sp != null);

        LecturerAttendancePanel ap = new LecturerAttendancePanel(s.access, s.lecturer);
        ap.refresh();
        check("attendance screen: 3 students, 93.33 % shown, missing attendance shown as 'Not recorded'",
                rows(ap) == 3 && cellsContain(table(ap), "93.33") && cellsContain(table(ap), "Not recorded"));

        LecturerMedicalPanel mp = new LecturerMedicalPanel(s.access, s.medical);
        mp.refresh();
        check("medical screen: rows shown, reason column says 'Confidential', privacy notice visible",
                rows(mp) == 2 && cellsContain(table(mp), "Confidential") && text(mp).contains("confidential")
                        && !text(mp).toLowerCase().contains("fever"));

        StudentsPanel st = new StudentsPanel(s.access, s.lecturer);
        st.refresh();
        check("undergraduate details: only my 3 students, never the other lecturer's", rows(st) == 3);

        CourseMaterialsPanel cm = new CourseMaterialsPanel(s.access, s.materials);
        cm.refresh();
        check("course materials: empty state message", text(cm).contains("No course materials yet"));

        Method wrap = Class.forName("com.facultyams.dao.JdbcSupport").getDeclaredMethod("wrap", java.sql.SQLException.class);
        wrap.setAccessible(true);
        String schemaMsg = ((DatabaseException) wrap.invoke(null,
                new java.sql.SQLException("Unknown column 'm.course_id' in 'where clause'", "42S22"))).getMessage();
        check("outdated / missing lecturer tables give guidance (migration 005 + LecturerDbCheck) and keep the database's own message",
                schemaMsg.contains("005_lecturer_module.sql") && schemaMsg.contains("LecturerDbCheck") && schemaMsg.contains("Unknown column"));
        check("missing table (MySQL 42S02) is recognised too", ((DatabaseException) wrap.invoke(null,
                new java.sql.SQLException("Table 'faculty_ams.marks' doesn't exist", "42S02"))).getMessage().contains("005_lecturer_module.sql"));
        check("other database errors are NOT mislabelled as a schema problem", !((DatabaseException) wrap.invoke(null,
                new java.sql.SQLException("Lock wait timeout exceeded", "40001"))).getMessage().contains("005_lecturer_module"));

        Session.end();
        EvaluationComponentsPanel denied = new EvaluationComponentsPanel(s.access, s.evaluation);
        denied.refresh();
        check("logged out: screens show an error and no data", rows(denied) == 0 && text(denied).contains("Could not complete"));
        sync.set(null, false);
    }

    // ---- headless component inspection ----

    private static List<Component> all(Container root) {
        List<Component> list = new ArrayList<>();
        for (Component c : root.getComponents()) {
            list.add(c);
            if (c instanceof Container k) {
                list.addAll(all(k));
            }
        }
        return list;
    }

    private static JTable table(JComponent root) {
        for (Component c : all(root)) {
            if (c instanceof JTable t) {
                return t;
            }
        }
        return null;
    }

    private static int rows(JComponent root) {
        JTable t = table(root);
        return t == null ? 0 : t.getRowCount();
    }

    private static String text(JComponent root) {
        StringBuilder sb = new StringBuilder();
        for (Component c : all(root)) {
            if (c instanceof JLabel l && l.getText() != null) {
                sb.append(l.getText()).append('\n');
            }
        }
        JTable t = table(root);
        if (t != null) {
            for (int r = 0; r < t.getRowCount(); r++) {
                for (int c = 0; c < t.getColumnCount(); c++) {
                    sb.append(t.getValueAt(r, c)).append(' ');
                }
            }
        }
        return sb.toString();
    }

    private static boolean cellsContain(JTable t, String value) {
        for (int r = 0; r < t.getRowCount(); r++) {
            for (int c = 0; c < t.getColumnCount(); c++) {
                if (value.equals(String.valueOf(t.getValueAt(r, c)))) {
                    return true;
                }
            }
        }
        return false;
    }
}
