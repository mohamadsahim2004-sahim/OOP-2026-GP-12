package com.facultyams.model;

/** A student as shown in drop-downs and search results. */
public record StudentInfo(int userId, String fullName, String registrationNo, String batch) {
    @Override
    public String toString() {
        return registrationNo + " - " + fullName;
    }
}