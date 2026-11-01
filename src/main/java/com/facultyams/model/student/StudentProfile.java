package com.facultyams.model.student;

import com.facultyams.model.profile.StudentStatus;

/**
 * Read model for the student's profile page, filled from {@code users}, {@code students} and
 * {@code departments}. Only the contact number and profile picture can be changed by the student
 * (see {@code Student.getEditableProfileFields()}); everything else is administered elsewhere.
 */
public class StudentProfile {
    private int userId;
    private String username;
    private String fullName;
    private String email;
    private String registrationNumber;
    private String batch;
    private String studentCategory;   // REGULAR / REPEAT / BATCH_MISSED as stored
    private String department;
    private String contactNumber;
    private String profilePicture;
    private StudentStatus status = StudentStatus.ACTIVE;
    private String currentLevelSemester = "";
    private int enrolledCourses;
    private int enrolledCredits;
    private double attendancePercentage = -1;   // -1 = no attendance recorded yet
    private boolean hasCgpa;
    private double cgpa;

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRegNo() { return registrationNumber; }
    public void setRegNo(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }
    public String getStudentCategory() { return studentCategory; }
    public void setStudentCategory(String studentCategory) { this.studentCategory = studentCategory; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    /** The university stores no separate degree programme, so the department name is shown. */
    public String getDegreeProgram() { return department == null ? "" : department; }
    public String getAcademicYear() { return batch == null ? "" : "Batch " + batch; }
    public String getCurrentSemester() { return currentLevelSemester; }
    public void setCurrentSemester(String value) { this.currentLevelSemester = value == null ? "" : value; }
    public String getPhoneNumber() { return contactNumber; }
    public void setPhoneNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public String getProfileImageUrl() { return profilePicture; }
    public void setProfileImageUrl(String profilePicture) { this.profilePicture = profilePicture; }
    public StudentStatus getStatus() { return status; }
    public void setStatus(StudentStatus status) { this.status = status; }
    public int getEnrolledCoursesCount() { return enrolledCourses; }
    public void setEnrolledCoursesCount(int enrolledCourses) { this.enrolledCourses = enrolledCourses; }
    public int getEnrolledCredits() { return enrolledCredits; }
    public void setEnrolledCredits(int enrolledCredits) { this.enrolledCredits = enrolledCredits; }
    public boolean hasAttendance() { return attendancePercentage >= 0; }
    public double getAttendancePercentage() { return Math.max(0, attendancePercentage); }
    public void setAttendancePercentage(double value) { this.attendancePercentage = value; }
    public boolean hasCgpa() { return hasCgpa; }
    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; this.hasCgpa = true; }
}
