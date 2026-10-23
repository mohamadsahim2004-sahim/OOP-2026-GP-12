package com.facultyams.ui.components;

import com.facultyams.model.Course;

public final class ElectivesTableModel extends EnrollmentTableModel {
    private static final String[] COLUMNS = {
        "Code", "Course Title", "Credits", "Lecturer", "Department", "Schedule", "", "Action"
    };

    public ElectivesTableModel() { super(COLUMNS); }

    @Override
    public Object getValueAt(int row, int column) {
        Course course = getCourseAt(row);
        return switch (column) {
            case 0 -> course.getCode();
            case 1 -> course.getTitle();
            case 2 -> Integer.toString(course.getCredits());
            case 3 -> course.getLecturerName();
            case 4 -> course.getDepartment();
            case 5 -> course.getScheduleTime();
            case 6 -> "";
            case 7 -> "Enroll";
            default -> "";
        };
    }
}
