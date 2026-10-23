package com.facultyams.service;

import com.facultyams.model.Course;
import com.facultyams.model.Schedule;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Validates and coordinates course enrollment through the existing data service. */
public final class EnrollmentService {
    public static final int MAX_CREDITS = 20;

    private final DataService dataService;
    private final List<EnrollmentListener> listeners = new ArrayList<>();

    public EnrollmentService(DataService dataService) {
        this.dataService = Objects.requireNonNull(dataService);
    }

    public void addEnrollmentListener(EnrollmentListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public List<Course> getEnrolled() { return dataService.getEnrolledCourses(); }
    public List<Course> getAvailable() { return dataService.getAvailableCourses(); }
    public int getTotalCredits() { return dataService.getTotalEnrolledCredits(); }

    public EnrollmentResult validateEnrollment(Course course) {
        if (course == null || course.isEnrolled()) {
            return EnrollmentResult.failure("This course is already enrolled.");
        }
        if (getTotalCredits() + course.getCredits() > MAX_CREDITS) {
            return EnrollmentResult.failure("This course would exceed the 20-credit limit.");
        }
        Schedule requested = Schedule.parse(course.getScheduleTime());
        if (requested != null) {
            for (Course enrolled : getEnrolled()) {
                Schedule existing = Schedule.parse(enrolled.getScheduleTime());
                if (requested.overlaps(existing)) {
                    return EnrollmentResult.failure("This course conflicts with " + enrolled.getCode()
                            + " on " + enrolled.getScheduleTime() + ".");
                }
            }
        }
        return EnrollmentResult.success("Course is available to enroll.");
    }

    public EnrollmentResult enroll(Course course) {
        EnrollmentResult validation = validateEnrollment(course);
        if (!validation.success()) return validation;
        if (!dataService.enrollCourse(course.getCode())) {
            return EnrollmentResult.failure("Could not enroll in " + course.getCode() + ".");
        }
        notifyListeners();
        return EnrollmentResult.success("Enrolled in " + course.getCode() + " successfully.");
    }

    public EnrollmentResult drop(Course course) {
        if (course == null || !course.isEnrolled()) {
            return EnrollmentResult.failure("This course is not currently enrolled.");
        }
        if (!dataService.dropCourse(course.getCode())) {
            return EnrollmentResult.failure("Could not drop " + course.getCode() + ".");
        }
        notifyListeners();
        return EnrollmentResult.success("Dropped " + course.getCode() + " successfully.");
    }

    private void notifyListeners() {
        for (EnrollmentListener listener : List.copyOf(listeners)) listener.enrollmentChanged();
    }
}
