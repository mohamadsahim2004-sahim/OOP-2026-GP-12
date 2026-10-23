package com.facultyams.ui.components;

import com.facultyams.model.Course;

public final class EnrolledTableModel extends EnrollmentTableModel {
    private static final String[] COLUMNS = {
        "Code", "Course Title", "Credits", "Lecturer", "Type", "Schedule", "Status", "Action"
    };

    public EnrolledTableModel() { super(COLUMNS); }

    @Override
    public Object getValueAt(int row, int column) {
        Course course = getCourseAt(row);
        return switch (column) {
            case 0 -> course.getCode();
            case 1 -> course.getTitle();
            case 2 -> Integer.toString(course.getCredits());
            case 3 -> course.getLecturerName();
            case 4 -> course.getCourseType();
            case 5 -> course.getScheduleTime();
            case 6 -> "Enrolled";
            case 7 -> "Drop";
            default -> "";
        };
    }
}
