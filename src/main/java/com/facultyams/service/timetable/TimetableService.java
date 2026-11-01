package com.facultyams.service.timetable;

import com.facultyams.model.timetable.ClassSession;
import com.facultyams.model.timetable.ClassType;
import com.facultyams.model.timetable.PersonalEvent;
import com.facultyams.model.timetable.ReminderOffset;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Persistence boundary for academic sessions, personal events, and reminder preferences. */
public interface TimetableService {
    List<ClassSession> getSessions();
    List<ClassSession> getSessions(DayOfWeek day);
    List<ClassSession> getSessionsForDate(LocalDate date);
    List<ClassSession> search(String query, ClassType type, DayOfWeek day);
    List<ClassSession> findClashes(PersonalEvent event);
    void saveEvent(PersonalEvent event);
    void deleteEvent(String id);
    Optional<PersonalEvent> getEvent(String id);
    boolean isReminderEnabled(String sessionId);
    void setReminderEnabled(String sessionId, boolean enabled);
    ReminderOffset getReminderOffset();
    void setReminderOffset(ReminderOffset offset);
}
