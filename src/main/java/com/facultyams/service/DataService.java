package com.facultyams.service;

import com.facultyams.model.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.CopyOnWriteArrayList;

public class DataService {
    private static DataService instance;

    private Student currentStudent;
    private final List<Marks> recentMarks = new ArrayList<>();
    private final List<Marks> allSemesterMarks = new ArrayList<>();
    private final List<Notice> notices = new ArrayList<>();
    private final List<Course> allCourses = new ArrayList<>();
    private final List<CourseMaterial> materials = new ArrayList<>();
    private final List<TimeTable> timetableEntries = new ArrayList<>();
    private final List<Attendance> attendanceRecords = new ArrayList<>();
    private final List<Medical> medicalRecords = new ArrayList<>();
    private final List<ProfileChangeListener> profileChangeListeners = new CopyOnWriteArrayList<>();

    private DataService() {
        initData();
    }

    public static synchronized DataService getInstance() {
        if (instance == null) {
            instance = new DataService();
        }
        return instance;
    }

    private void initData() {
        // Current Student matching the screenshot
        currentStudent = new Student(1, "abhishek", "Abhishek Verma", "abhishek.verma@eng.university.ac.lk",
                "TG/2021/842", "B.Tech CSE - 3rd Year", "2021/2022", 3.45);
        currentStudent.setCurrentSemester("Semester 5");
        currentStudent.setOverallRank(12);
        currentStudent.setTotalStudents(120);
        currentStudent.setAttendancePercentage(87.0);
        currentStudent.setCompletedExams(4);
        currentStudent.setEnrolledCoursesCount(6);
        currentStudent.setDepartment("Computer Science & Engineering");
        currentStudent.setAcademicAdvisor("Prof. K. R. Jayawardena");
        currentStudent.setPhoneNumber("+94 77 123 4567");
        currentStudent.setAddress("No. 45, University Crescent, Colombo 07");
        currentStudent.setEmergencyContact("+94 71 987 6543 (Parent)");
        currentStudent.setPersonalEmail("abhishek.personal@gmail.com");
        currentStudent.setEmergencyRelationship("Parent");
        currentStudent.setProfileImageUrl("/images/profile.png");

        // Recent Marks matching screenshot
        recentMarks.add(new Marks(1, "CS301", "Data Structures", "Mid Sem", "A", 86, 100, "12 May 2024", 4, "Semester 5"));
        recentMarks.add(new Marks(2, "CS302", "Database Management Systems", "Mid Sem", "A+", 91, 100, "10 May 2024", 3, "Semester 5"));
        recentMarks.add(new Marks(3, "CS303", "Operating Systems", "Mid Sem", "B+", 78, 100, "08 May 2024", 3, "Semester 5"));
        recentMarks.add(new Marks(4, "CS304", "Computer Networks", "Mid Sem", "A", 85, 100, "06 May 2024", 3, "Semester 5"));
        recentMarks.add(new Marks(5, "CS305", "Software Engineering", "Mid Sem", "A+", 92, 100, "04 May 2024", 3, "Semester 5"));

        // All Marks history
        allSemesterMarks.addAll(recentMarks);
        allSemesterMarks.add(new Marks(6, "CS201", "Object Oriented Programming", "End Sem", "A", 88, 100, "15 Dec 2023", 4, "Semester 4"));
        allSemesterMarks.add(new Marks(7, "CS202", "Computer Architecture", "End Sem", "A-", 81, 100, "12 Dec 2023", 3, "Semester 4"));
        allSemesterMarks.add(new Marks(8, "MA201", "Discrete Mathematics", "End Sem", "B+", 76, 100, "09 Dec 2023", 3, "Semester 4"));
        allSemesterMarks.add(new Marks(9, "CS101", "Intro to Programming", "End Sem", "A+", 94, 100, "20 Jun 2023", 4, "Semester 3"));
        allSemesterMarks.add(new Marks(10, "MA101", "Engineering Mathematics", "End Sem", "A", 85, 100, "17 Jun 2023", 4, "Semester 3"));

        // Notices matching screenshot
        notices.add(new Notice(1, "End Semester Exam Schedule", 
                "The end semester exams will begin from 25th May 2024. All undergraduate students are requested to review their registered examination admissions and hall allocations.", 
                "15 May 2024", "Examinations", "Senior Assistant Registrar / Exams", true));
        notices.add(new Notice(2, "Result Declaration Notice", 
                "Results for Mid Semester Exams has been declared. Registered candidates can check their individual mark summaries and request verification before 20th May.", 
                "12 May 2024", "Academic", "Office of the Dean", true));
        notices.add(new Notice(3, "Project Submission Deadline", 
                "Final year and 3rd year mini project submission is due on 30th May 2024. Please submit through the LMS repository by 23:59 hrs.", 
                "10 May 2024", "Academic", "Project Coordinator", true));
        notices.add(new Notice(4, "Guest Lecture: Cloud Systems & Microservices", 
                "Industry expert talk by Senior Principal Architect, AWS on containerization and Kubernetes orchestration. Date: 18 May at Main Auditorium.", 
                "07 May 2024", "General", "CSE Student Society", false));
        notices.add(new Notice(5, "Inter-Faculty Sports Meet 2024", 
                "Track and field events registration opens today at the Gymnasium Office. Practice sessions start everyday 4:30 PM.", 
                "03 May 2024", "Events", "Physical Education Dept", false));

        // Courses (Enrolled + Available)
        allCourses.add(new Course("CS301", "Data Structures", 4, "CSE", "Semester 5", "Dr. A. Fernando", "Core", true, "Mon 08:30 - 10:30"));
        allCourses.add(new Course("CS302", "Database Management Systems", 3, "CSE", "Semester 5", "Prof. S. Perera", "Core", true, "Tue 08:30 - 10:30"));
        allCourses.add(new Course("CS303", "Operating Systems", 3, "CSE", "Semester 5", "Dr. N. Rajapakse", "Core", true, "Wed 08:30 - 11:30"));
        allCourses.add(new Course("CS304", "Computer Networks", 3, "CSE", "Semester 5", "Dr. M. Silva", "Core", true, "Tue 11:00 - 13:00"));
        allCourses.add(new Course("CS305", "Software Engineering", 3, "CSE", "Semester 5", "Mrs. T. Wickramasinghe", "Core", true, "Mon 10:45 - 12:45"));
        allCourses.add(new Course("CS306", "Web Application Development", 2, "CSE", "Semester 5", "Mr. R. Kumara", "Elective", true, "Mon 13:30 - 15:30"));

        // Electives available for enrollment
        allCourses.add(new Course("CS307", "Mobile Application Development", 2, "CSE", "Semester 5", "Dr. D. Wijesinghe", "Elective", false, "Thu 14:00 - 16:00"));
        allCourses.add(new Course("CS308", "Artificial Intelligence Fundamentals", 3, "CSE", "Semester 5", "Prof. C. Senaratne", "Elective", false, "Fri 14:00 - 17:00"));
        allCourses.add(new Course("CS309", "Cyber Security & Cryptography", 3, "CSE", "Semester 5", "Dr. K. Pathirana", "Elective", false, "Thu 08:30 - 11:30"));
        allCourses.add(new Course("MG301", "Entrepreneurship & Innovation", 2, "Management", "Semester 5", "Dr. H. Mendis", "General", false, "Wed 14:00 - 16:00"));

        // Course Materials
        materials.add(new CourseMaterial(1, "CS301", "Data Structures", "Lecture 01 - Binary Search Trees & AVL Trees.pdf", "Lecture Notes", "2.4 MB", "10 May 2024", "Dr. A. Fernando"));
        materials.add(new CourseMaterial(2, "CS301", "Data Structures", "Lab Sheet 04 - Graph Traversal & Dijkstra in Java.pdf", "Lab Sheet", "1.1 MB", "08 May 2024", "Dr. A. Fernando"));
        materials.add(new CourseMaterial(3, "CS302", "Database Management Systems", "Lecture 05 - Relational Algebra & Query Trees.pdf", "Lecture Notes", "3.8 MB", "09 May 2024", "Prof. S. Perera"));
        materials.add(new CourseMaterial(4, "CS302", "Database Management Systems", "Assignment 02 - Normalization & Schema Design.docx", "Assignment", "850 KB", "07 May 2024", "Prof. S. Perera"));
        materials.add(new CourseMaterial(5, "CS303", "Operating Systems", "Lecture 07 - Process Synchronization & Semaphores.pdf", "Lecture Notes", "4.1 MB", "06 May 2024", "Dr. N. Rajapakse"));
        materials.add(new CourseMaterial(6, "CS303", "Operating Systems", "Past Paper & Solutions - 2023 Mid Sem.pdf", "Past Paper", "1.5 MB", "02 May 2024", "Dr. N. Rajapakse"));
        materials.add(new CourseMaterial(7, "CS304", "Computer Networks", "Lecture 08 - TCP/IP Protocol Suite & Subnetting.pdf", "Lecture Notes", "3.2 MB", "05 May 2024", "Dr. M. Silva"));
        materials.add(new CourseMaterial(8, "CS305", "Software Engineering", "Lecture 06 - Agile Methodologies & Scrum Framework.pdf", "Lecture Notes", "2.8 MB", "04 May 2024", "Mrs. T. Wickramasinghe"));
        materials.add(new CourseMaterial(9, "CS305", "Software Engineering", "SRS Specification Template & IEEE Guideline.docx", "Assignment", "620 KB", "03 May 2024", "Mrs. T. Wickramasinghe"));
        materials.add(new CourseMaterial(10, "CS306", "Web Application Development", "Lecture 04 - Modern Front-End Frameworks & REST.pdf", "Lecture Notes", "2.1 MB", "01 May 2024", "Mr. R. Kumara"));

        // Timetable entries
        timetableEntries.add(new TimeTable(1, "Monday", "08:30", "10:30", "CS301", "Data Structures", "Lab 02 (Computer Centre)", "Dr. A. Fernando", "Practical"));
        timetableEntries.add(new TimeTable(2, "Monday", "10:45", "12:45", "CS305", "Software Engineering", "Lecture Hall 4A", "Mrs. T. Wickramasinghe", "Lecture"));
        timetableEntries.add(new TimeTable(3, "Monday", "13:30", "15:30", "CS306", "Web Application Development", "Lab 01 (Networking Lab)", "Mr. R. Kumara", "Practical"));

        timetableEntries.add(new TimeTable(4, "Tuesday", "08:30", "10:30", "CS302", "Database Management Systems", "Lecture Hall 2B", "Prof. S. Perera", "Lecture"));
        timetableEntries.add(new TimeTable(5, "Tuesday", "11:00", "13:00", "CS304", "Computer Networks", "Lecture Hall 1A", "Dr. M. Silva", "Lecture"));
        timetableEntries.add(new TimeTable(6, "Tuesday", "14:00", "16:00", "CS302", "DBMS Lab Practical", "Lab 03 (Database Lab)", "Prof. S. Perera", "Practical"));

        timetableEntries.add(new TimeTable(7, "Wednesday", "08:30", "11:30", "CS303", "Operating Systems", "Lecture Hall 3B", "Dr. N. Rajapakse", "Lecture"));
        timetableEntries.add(new TimeTable(8, "Wednesday", "13:30", "15:30", "CS301", "Data Structures Tutorial", "Room 204 (Seminar Block)", "Dr. A. Fernando", "Tutorial"));

        timetableEntries.add(new TimeTable(9, "Thursday", "09:00", "11:00", "CS304", "Computer Networks Lab", "Networking Lab (3rd Floor)", "Dr. M. Silva", "Practical"));
        timetableEntries.add(new TimeTable(10, "Thursday", "11:30", "13:30", "CS305", "Software Engineering Tutorial", "Room 102", "Mrs. T. Wickramasinghe", "Tutorial"));

        timetableEntries.add(new TimeTable(11, "Friday", "08:30", "10:30", "CS303", "Operating Systems Lab", "Lab 02 (OS Lab)", "Dr. N. Rajapakse", "Practical"));
        timetableEntries.add(new TimeTable(12, "Friday", "10:45", "12:45", "CS306", "Web Application Dev Tutorial", "Lecture Hall 1B", "Mr. R. Kumara", "Lecture"));

        // Attendance
        attendanceRecords.add(new Attendance("CS301", "Data Structures", 32, 28, "10 May 2024"));
        attendanceRecords.add(new Attendance("CS302", "Database Management Systems", 26, 24, "09 May 2024"));
        attendanceRecords.add(new Attendance("CS303", "Operating Systems", 26, 22, "08 May 2024"));
        attendanceRecords.add(new Attendance("CS304", "Computer Networks", 26, 23, "07 May 2024"));
        attendanceRecords.add(new Attendance("CS305", "Software Engineering", 24, 21, "06 May 2024"));
        attendanceRecords.add(new Attendance("CS306", "Web Application Development", 18, 14, "03 May 2024"));

        // Medical
        medicalRecords.add(new Medical("MED-2024-001", "TG/2021/842", "CS303", "Operating Systems", 
                "2024-04-10", "2024-04-12", "Viral Fever & Fatigue", "Dr. M. K. Bandara", 
                "Colombo General Teaching Hospital", "2024-04-14", "Approved", "Medical board verified and accepted for mid term absence."));
        medicalRecords.add(new Medical("MED-2024-002", "TG/2021/842", "CS306", "Web Application Development", 
                "2024-05-02", "2024-05-03", "Severe Migraine & Eye Strain", "Dr. S. N. Alwis", 
                "City Care Healthcare Center", "2024-05-04", "Pending", "Under review by Senior Student Counselor."));
    }

    public Student getCurrentStudent() {
        return currentStudent;
    }

    public List<Marks> getRecentMarks() {
        return Collections.unmodifiableList(recentMarks);
    }

    public List<Marks> getAllSemesterMarks() {
        return Collections.unmodifiableList(allSemesterMarks);
    }

    public List<Notice> getNotices() {
        return Collections.unmodifiableList(notices);
    }

    public List<Course> getAllCourses() {
        return Collections.unmodifiableList(allCourses);
    }

    public List<Course> getEnrolledCourses() {
        return allCourses.stream().filter(Course::isEnrolled).collect(Collectors.toList());
    }

    public List<Course> getAvailableCourses() {
        return allCourses.stream().filter(c -> !c.isEnrolled()).collect(Collectors.toList());
    }

    public boolean enrollCourse(String courseCode) {
        for (Course c : allCourses) {
            if (c.getCode().equalsIgnoreCase(courseCode) && !c.isEnrolled()) {
                c.setEnrolled(true);
                currentStudent.setEnrolledCoursesCount((int) allCourses.stream().filter(Course::isEnrolled).count());
                return true;
            }
        }
        return false;
    }

    public boolean dropCourse(String courseCode) {
        for (Course c : allCourses) {
            if (c.getCode().equalsIgnoreCase(courseCode) && c.isEnrolled()) {
                c.setEnrolled(false);
                currentStudent.setEnrolledCoursesCount((int) allCourses.stream().filter(Course::isEnrolled).count());
                return true;
            }
        }
        return false;
    }

    public List<CourseMaterial> getMaterials() {
        return Collections.unmodifiableList(materials);
    }

    public List<CourseMaterial> getMaterialsForCourse(String courseCode) {
        if (courseCode == null || courseCode.equalsIgnoreCase("ALL")) {
            return getMaterials();
        }
        return materials.stream().filter(m -> m.getCourseCode().equalsIgnoreCase(courseCode)).collect(Collectors.toList());
    }

    public List<TimeTable> getTimetableEntries() {
        return Collections.unmodifiableList(timetableEntries);
    }

    public List<TimeTable> getTimetableForDay(String day) {
        if (day == null || day.equalsIgnoreCase("ALL")) {
            return getTimetableEntries();
        }
        return timetableEntries.stream().filter(t -> t.getDayOfWeek().equalsIgnoreCase(day)).collect(Collectors.toList());
    }

    public List<Attendance> getAttendanceRecords() {
        return Collections.unmodifiableList(attendanceRecords);
    }

    public List<Medical> getMedicalRecords() {
        return Collections.unmodifiableList(medicalRecords);
    }

    public void addMedicalRecord(Medical med) {
        medicalRecords.add(0, med);
    }

    public void updateStudentProfile(String email, String phone, String address, String emergencyContact) {
        currentStudent.setPersonalEmail(email);
        currentStudent.setPhoneNumber(phone);
        currentStudent.setAddress(address);
        currentStudent.setEmergencyContact(emergencyContact);
        fireProfileChanged();
    }

    public void updateStudentContactInfo(String personalEmail, String phone, String address,
                                         String emergencyNumber, String relationship) {
        currentStudent.setPersonalEmail(personalEmail);
        currentStudent.setPhoneNumber(phone);
        currentStudent.setAddress(address);
        currentStudent.setEmergencyContact(emergencyNumber);
        currentStudent.setEmergencyRelationship(relationship);
        fireProfileChanged();
    }

    public void updateStudentPhoto(String imagePath) {
        currentStudent.setProfileImageUrl(imagePath);
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
            listener.profileChanged(currentStudent);
        }
    }

    public int getTotalEnrolledCredits() {
        return getEnrolledCourses().stream().mapToInt(Course::getCredits).sum();
    }
}
