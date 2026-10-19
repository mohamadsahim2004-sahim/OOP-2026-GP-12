package com.srms.service;

import com.srms.model.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mock attendance service initialized with real dated sessions over the past 3 weeks
 * matching the user's screenshot exactly.
 */
public class MockAttendanceService implements AttendanceService {
    private final List<CourseAttendance> courseSummaries = new ArrayList<>();
    private final List<SessionAttendance> allSessions = new ArrayList<>();
    private final List<AttendanceListener> listeners = new CopyOnWriteArrayList<>();

    public MockAttendanceService() {
        seedData();
    }

    private void seedData() {
        // Course summaries matching the screenshot
        CourseAttendance c1 = new CourseAttendance("CS301", "Data Structures", 32, 28, 0);
        CourseAttendance c2 = new CourseAttendance("CS302", "Database Management Systems", 26, 24, 0);
        CourseAttendance c3 = new CourseAttendance("CS303", "Operating Systems", 26, 22, 0);
        CourseAttendance c4 = new CourseAttendance("CS304", "Computer Networks", 26, 23, 0);
        CourseAttendance c5 = new CourseAttendance("CS305", "Software Engineering", 24, 21, 0);
        CourseAttendance c6 = new CourseAttendance("CS306", "Web Application Development", 18, 14, 0);

        courseSummaries.addAll(List.of(c1, c2, c3, c4, c5, c6));

        // Generate dated sessions over the past 21 days
        LocalDate today = LocalDate.now();
        int absentC1 = 4, absentC2 = 2, absentC3 = 4, absentC4 = 3, absentC5 = 3, absentC6 = 4;

        int idCounter = 1;
        for (int i = 20; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            DayOfWeek dow = d.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;

            if (dow == DayOfWeek.MONDAY) {
                // CS301 (08:30-10:30) & CS305 (10:45-12:45) & CS306 (13:30-15:30)
                SessionStatus st1 = (absentC1-- > 0) ? SessionStatus.ABSENT : SessionStatus.PRESENT;
                allSessions.add(new SessionAttendance(new ClassSession("sess_" + (idCounter++), "CS301", "Data Structures", d, LocalTime.of(8, 30), LocalTime.of(10, 30), "Lecture", "Dr. A. Fernando"), st1));

                SessionStatus st5 = (absentC5-- > 0) ? SessionStatus.ABSENT : SessionStatus.PRESENT;
                allSessions.add(new SessionAttendance(new ClassSession("sess_" + (idCounter++), "CS305", "Software Engineering", d, LocalTime.of(10, 45), LocalTime.of(12, 45), "Lecture", "Mrs. T. Wickramasinghe"), st5));

                SessionStatus st6 = (absentC6-- > 0) ? SessionStatus.ABSENT : SessionStatus.PRESENT;
                allSessions.add(new SessionAttendance(new ClassSession("sess_" + (idCounter++), "CS306", "Web Application Development", d, LocalTime.of(13, 30), LocalTime.of(15, 30), "Practical", "Mr. R. Kumara"), st6));
            } else if (dow == DayOfWeek.TUESDAY) {
                // CS302 (08:30-10:30) & CS304 (11:00-13:00)
                SessionStatus st2 = (absentC2-- > 0) ? SessionStatus.ABSENT : SessionStatus.PRESENT;
                allSessions.add(new SessionAttendance(new ClassSession("sess_" + (idCounter++), "CS302", "Database Management Systems", d, LocalTime.of(8, 30), LocalTime.of(10, 30), "Lecture", "Prof. S. Perera"), st2));

                SessionStatus st4 = (absentC4-- > 0) ? SessionStatus.ABSENT : SessionStatus.PRESENT;
                allSessions.add(new SessionAttendance(new ClassSession("sess_" + (idCounter++), "CS304", "Computer Networks", d, LocalTime.of(11, 0), LocalTime.of(13, 0), "Lecture", "Dr. M. Silva"), st4));
            } else if (dow == DayOfWeek.WEDNESDAY) {
                // CS303 (08:30-11:30)
                SessionStatus st3 = (absentC3-- > 0) ? SessionStatus.ABSENT : SessionStatus.PRESENT;
                allSessions.add(new SessionAttendance(new ClassSession("sess_" + (idCounter++), "CS303", "Operating Systems", d, LocalTime.of(8, 30), LocalTime.of(11, 30), "Practical", "Dr. N. Rajapakse"), st3));
            } else if (dow == DayOfWeek.THURSDAY) {
                // CS301 (08:30-10:30) & CS306 (14:00-16:00)
                SessionStatus st1 = (absentC1-- > 0) ? SessionStatus.ABSENT : SessionStatus.PRESENT;
                allSessions.add(new SessionAttendance(new ClassSession("sess_" + (idCounter++), "CS301", "Data Structures", d, LocalTime.of(8, 30), LocalTime.of(10, 30), "Practical", "Dr. A. Fernando"), st1));

                SessionStatus st6 = (absentC6-- > 0) ? SessionStatus.ABSENT : SessionStatus.PRESENT;
                allSessions.add(new SessionAttendance(new ClassSession("sess_" + (idCounter++), "CS306", "Web Application Development", d, LocalTime.of(14, 0), LocalTime.of(16, 0), "Lecture", "Mr. R. Kumara"), st6));
            } else if (dow == DayOfWeek.FRIDAY) {
                // CS302 & CS303
                SessionStatus st2 = (absentC2-- > 0) ? SessionStatus.ABSENT : SessionStatus.PRESENT;
                allSessions.add(new SessionAttendance(new ClassSession("sess_" + (idCounter++), "CS302", "Database Management Systems", d, LocalTime.of(9, 0), LocalTime.of(11, 0), "Practical", "Prof. S. Perera"), st2));

                SessionStatus st3 = (absentC3-- > 0) ? SessionStatus.ABSENT : SessionStatus.PRESENT;
                allSessions.add(new SessionAttendance(new ClassSession("sess_" + (idCounter++), "CS303", "Operating Systems", d, LocalTime.of(13, 30), LocalTime.of(15, 30), "Lecture", "Dr. N. Rajapakse"), st3));
            }
        }
    }

    @Override
    public synchronized List<CourseAttendance> summary(String studentId) {
        return new ArrayList<>(courseSummaries);
    }

    @Override
    public synchronized List<SessionAttendance> sessionsBetween(String studentId, LocalDate from, LocalDate to) {
        List<SessionAttendance> result = new ArrayList<>();
        for (SessionAttendance sa : allSessions) {
            LocalDate d = sa.session().date();
            if (!d.isBefore(from) && !d.isAfter(to)) {
                result.add(sa);
            }
        }
        return result;
    }

    @Override
    public synchronized List<SessionAttendance> sessionsForCourse(String studentId, String courseCode) {
        List<SessionAttendance> result = new ArrayList<>();
        for (SessionAttendance sa : allSessions) {
            if (sa.session().courseCode().equalsIgnoreCase(courseCode)) {
                result.add(sa);
            }
        }
        return result;
    }

    @Override
    public synchronized void markSessionsMedicalApproved(List<String> sessionIds) {
        Set<String> idSet = new HashSet<>(sessionIds);
        Map<String, Integer> approvedCounts = new HashMap<>();

        for (int i = 0; i < allSessions.size(); i++) {
            SessionAttendance sa = allSessions.get(i);
            if (idSet.contains(sa.session().id())) {
                allSessions.set(i, new SessionAttendance(sa.session(), SessionStatus.MEDICAL_APPROVED));
                approvedCounts.merge(sa.session().courseCode(), 1, Integer::sum);
            }
        }

        // Update courseSummaries
        for (int i = 0; i < courseSummaries.size(); i++) {
            CourseAttendance ca = courseSummaries.get(i);
            int added = approvedCounts.getOrDefault(ca.courseCode(), 0);
            if (added > 0) {
                CourseAttendance updated = new CourseAttendance(
                        ca.courseCode(),
                        ca.courseTitle(),
                        ca.totalSessions(),
                        ca.attended(),
                        ca.medicalApproved() + added
                );
                courseSummaries.set(i, updated);
            }
        }

        notifyListeners();
    }

    @Override
    public void addAttendanceListener(AttendanceListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    private void notifyListeners() {
        for (AttendanceListener l : listeners) {
            try {
                l.onAttendanceUpdated();
            } catch (Exception ignored) {}
        }
    }
}
