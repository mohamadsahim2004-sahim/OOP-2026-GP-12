package com.facultyams.model;

import com.facultyams.security.Role;
import com.facultyams.model.profile.StudentStatus;
import com.facultyams.util.Constants;

public class Student extends User {
    private String regNo;
    private String degreeProgram;
    private String academicYear;
    private String currentSemester;
    private double cgpa;
    private int overallRank;
    private int totalStudents;
    private double attendancePercentage;
    private int completedExams;
    private int enrolledCoursesCount;
    private String address;
    private String academicAdvisor;
    private String department;
    private String emergencyContact;
    private String personalEmail;
    private String emergencyRelationship = "Parent";
    private int creditsCompleted = 78;
    private StudentStatus status = StudentStatus.ACTIVE;

    public Student() {
        setRole(Role.STUDENT);
    }

    public Student(int id, String username, String fullName, String email, String regNo, 
                   String degreeProgram, String academicYear, double cgpa) {
        super(id, username, fullName, email, Role.STUDENT);
        this.regNo = regNo;
        this.degreeProgram = degreeProgram;
        this.academicYear = academicYear;
        setCgpa(cgpa);
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getDegreeProgram() {
        return degreeProgram;
    }

    public void setDegreeProgram(String degreeProgram) {
        this.degreeProgram = degreeProgram;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getCurrentSemester() {
        return currentSemester;
    }

    public void setCurrentSemester(String currentSemester) {
        this.currentSemester = currentSemester;
    }

    public double getCgpa() {
        return cgpa;
    }

    public void setCgpa(double cgpa) {
        if (!Double.isFinite(cgpa) || cgpa < 0.0 || cgpa > Constants.MAX_GPA) {
            throw new IllegalArgumentException("CGPA must be between 0.00 and " + Constants.MAX_GPA + ".");
        }
        this.cgpa = cgpa;
    }

    public int getOverallRank() {
        return overallRank;
    }

    public void setOverallRank(int overallRank) {
        this.overallRank = overallRank;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public int getCompletedExams() {
        return completedExams;
    }

    public void setCompletedExams(int completedExams) {
        this.completedExams = completedExams;
    }

    public int getEnrolledCoursesCount() {
        return enrolledCoursesCount;
    }

    public void setEnrolledCoursesCount(int enrolledCoursesCount) {
        this.enrolledCoursesCount = enrolledCoursesCount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAcademicAdvisor() {
        return academicAdvisor;
    }

    public void setAcademicAdvisor(String academicAdvisor) {
        this.academicAdvisor = academicAdvisor;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getPersonalEmail() {
        return personalEmail;
    }

    public void setPersonalEmail(String personalEmail) {
        this.personalEmail = personalEmail;
    }

    public String getEmergencyRelationship() {
        return emergencyRelationship;
    }

    public void setEmergencyRelationship(String emergencyRelationship) {
        this.emergencyRelationship = emergencyRelationship;
    }

    public int getCreditsCompleted() {
        return creditsCompleted;
    }

    public void setCreditsCompleted(int creditsCompleted) {
        this.creditsCompleted = creditsCompleted;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
    }
}
