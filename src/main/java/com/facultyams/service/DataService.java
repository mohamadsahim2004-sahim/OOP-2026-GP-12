package com.facultyams.service;

import com.facultyams.dao.StudentMaterialDAO;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.CourseOffering;
import com.facultyams.model.student.StudentMaterial;
import com.facultyams.model.student.StudentNotice;
import com.facultyams.model.student.StudentProfile;
import com.facultyams.security.StudentAccess;
import com.facultyams.service.ResultService.Summary;
import com.facultyams.service.profile.DbStudentProfileService;
import com.facultyams.service.profile.StudentProfileService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Facade the undergraduate screens use to reach the database-backed services. One instance exists per
 * logged-in student (a different login gets a fresh one), so nothing is shared between students.
 * Every call re-checks the session, so a logged-out or non-student caller is rejected.
 */
public final class DataService {
    private static DataService instance;
    private static int instanceOwner = -1;

    private final int studentId;
    private final StudentProfileService profileService = new DbStudentProfileService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final StudentMaterialDAO materialDAO = new StudentMaterialDAO();
    private final List<ProfileChangeListener> profileChangeListeners = new CopyOnWriteArrayList<>();
    private StudentProfile cachedProfile;

    private DataService(int studentId) {
        this.studentId = studentId;
    }

    public static synchronized DataService getInstance() {
        int me = StudentAccess.currentStudentId();
        if (instance == null || instanceOwner != me) {
            instance = new DataService(me);
            instanceOwner = me;
        }
        return instance;
    }

    /** The logged-in student's profile with summary figures (cached until {@link #refreshProfile()}). */
    public synchronized StudentProfile getCurrentStudent() {
        StudentAccess.requireOwn(String.valueOf(studentId));
        if (cachedProfile == null) {
            cachedProfile = loadProfile();
        }
        return cachedProfile;
    }

    public synchronized StudentProfile refreshProfile() {
        cachedProfile = null;
        return getCurrentStudent();
    }

    private StudentProfile loadProfile() {
        StudentProfile p = profileService.getProfile();
        List<CourseOffering> enrolled = enrollmentService.getEnrolled();
        p.setEnrolledCoursesCount(enrolled.size());
        p.setEnrolledCredits(enrolled.stream().mapToInt(CourseOffering::getCredits).sum());
        // "Level 2 Semester 1" of the most advanced enrolled course
        enrolled.stream().map(CourseOffering::getSemester).max(String::compareTo).ifPresent(p::setCurrentSemester);
        Summary summary = ResultService.getInstance().getSummary();
        if (summary.hasResults()) {
            p.setCgpa(summary.cgpa().doubleValue());
        }
        var attendance = new DbStudentAttendanceService().summary(String.valueOf(studentId));
        int recorded = attendance.stream().mapToInt(com.facultyams.model.CourseAttendance::recorded).sum();
        if (recorded > 0) {
            double total = attendance.stream().filter(com.facultyams.model.CourseAttendance::hasRecords)
                    .mapToDouble(com.facultyams.model.CourseAttendance::percent).average().orElse(0);
            p.setAttendancePercentage(total);
        }
        return p;
    }

    public List<StudentNotice> getNotices() {
        return StudentNoticeService.getInstance().getNotices();
    }

    public List<CourseOffering> getEnrolledCourses() {
        return enrollmentService.getEnrolled();
    }

    public int getTotalEnrolledCredits() {
        return enrollmentService.getTotalCredits();
    }

    /** Materials of the student's enrolled courses only; a null / "ALL" code means every enrolled course. */
    public List<StudentMaterial> getMaterialsForCourse(String courseCode) {
        StudentAccess.requireOwn(String.valueOf(studentId));
        try {
            List<StudentMaterial> all = materialDAO.findForEnrolledCourses(studentId);
            if (courseCode == null || courseCode.equalsIgnoreCase("ALL")) return all;
            return all.stream().filter(m -> m.getCourseCode().equalsIgnoreCase(courseCode)).toList();
        } catch (DatabaseException e) {
            throw new StudentServiceException("Course materials could not be loaded. Please try again.", e);
        }
    }

    /** One material, only if it belongs to a course the student is enrolled in; otherwise null. */
    public StudentMaterial getMaterial(int materialId) {
        StudentAccess.requireOwn(String.valueOf(studentId));
        try {
            return materialDAO.findOne(studentId, materialId);
        } catch (DatabaseException e) {
            throw new StudentServiceException("The material could not be loaded. Please try again.", e);
        }
    }

    public void updateStudentContactNumber(String phone) {
        profileService.updateContactNumber(phone);
        refreshProfile();
        fireProfileChanged();
    }

    public void updateStudentPhoto(String imagePath) {
        profileService.updatePhoto(imagePath);
        refreshProfile();
        fireProfileChanged();
    }

    public void addProfileChangeListener(ProfileChangeListener listener) {
        profileChangeListeners.add(listener);
    }

    public void removeProfileChangeListener(ProfileChangeListener listener) {
        profileChangeListeners.remove(listener);
    }

    private void fireProfileChanged() {
        for (ProfileChangeListener listener : profileChangeListeners) {
            listener.profileChanged(cachedProfile);
        }
    }
}
