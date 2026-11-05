package com.facultyams.model;

import com.facultyams.security.Role;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Undergraduate student. May only update contact details and profile picture.
 * Student-facing features are implemented by Member 4.
 */
public class Student extends User {

    /** Regular, repeat or batch-missed undergraduate. Matches students.student_status. */
    public enum StudentStatus {
        REGULAR("Regular"),
        REPEAT("Repeat"),
        BATCH_MISSED("Batch missed");

        private final String displayName;

        StudentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final Set<ProfileField> EDITABLE = Collections.unmodifiableSet(
            EnumSet.of(ProfileField.CONTACT_NUMBER, ProfileField.DATE_OF_BIRTH, ProfileField.PROFILE_PICTURE));

    private String registrationNumber;
    private String batch;
    private LocalDate dateOfBirth;
    private StudentStatus studentStatus = StudentStatus.REGULAR;

    public Student() {
        super();
    }

    public Student(int userId, String username, String fullName, String email,
                   String registrationNumber, String batch, StudentStatus studentStatus) {
        super(userId, username, fullName, email);
        this.registrationNumber = registrationNumber;
        this.batch = batch;
        this.studentStatus = studentStatus;
    }

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    @Override
    public Set<ProfileField> getEditableProfileFields() {
        return EDITABLE;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /** Calculates age from date of birth; age is deliberately not stored. */
    public int getAge() {
        return calculateAge(dateOfBirth);
    }

    public static int calculateAge(LocalDate dateOfBirth) {
        return dateOfBirth == null ? 0 : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public StudentStatus getStudentStatus() {
        return studentStatus;
    }

    public void setStudentStatus(StudentStatus studentStatus) {
        this.studentStatus = studentStatus;
    }
}