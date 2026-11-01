package com.facultyams.model;

/** One teachable component (subject part) of a course: THEORY or PRACTICAL. */
public record ComponentInfo(int componentId, int courseId, String type) {
    @Override
    public String toString() {
        return type.charAt(0) + type.substring(1).toLowerCase();
    }
}