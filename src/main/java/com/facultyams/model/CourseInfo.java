package com.facultyams.model;

/** A course offered in the attendance drop-downs. */
public record CourseInfo(int courseId, String code, String name, String courseType) {
    @Override
    public String toString() {
        return code + " - " + name;
    }
}