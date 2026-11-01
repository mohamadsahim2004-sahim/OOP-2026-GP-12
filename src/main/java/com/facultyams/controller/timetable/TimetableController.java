package com.facultyams.controller.timetable;

import com.facultyams.model.timetable.*;
import com.facultyams.service.timetable.*;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Coordinates timetable actions without placing business rules in Swing components. */
public final class TimetableController {
    private final TimetableService service;
    private final Exportable exporter;
    private final ReminderService reminders;

    public TimetableController(TimetableService service, Exportable exporter) {
        this.service = service;
        this.exporter = exporter;
        this.reminders = new ScheduledReminderService(service);
    }
    public TimetableService getService() { return service; }
    public ReminderService getReminderService() { return reminders; }
    public List<ClassSession> findSessions(String query, ClassType type, DayOfWeek day) { return service.search(query, type, day); }
    public List<ClassSession> findSessionsForDate(String query, ClassType type, LocalDate date) {
        String term = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        return service.getSessionsForDate(date).stream()
                .filter(s -> type == null || s.getType() == type)
                .filter(s -> term.isEmpty()
                        || s.getCourse().getCode().toLowerCase(java.util.Locale.ROOT).contains(term)
                        || s.getDisplayName().toLowerCase(java.util.Locale.ROOT).contains(term)
                        || s.getLecturer().getName().toLowerCase(java.util.Locale.ROOT).contains(term)
                        || s.getVenue().getName().toLowerCase(java.util.Locale.ROOT).contains(term))
                .toList();
    }
    public List<ClassSession> findClashes(PersonalEvent event) { return service.findClashes(event); }
    public void saveEvent(PersonalEvent event) { service.saveEvent(event); }
    public void updateEvent(String id, String title, DayOfWeek day, java.time.LocalTime start,
                            java.time.LocalTime end, String venue, String note, boolean reminder) {
        service.saveEvent(new PersonalEvent(id, title, day, start, end, venue, note, reminder));
    }
    public void updateEvent(String id, String title, LocalDate date, java.time.LocalTime start,
                            java.time.LocalTime end, String venue, String note, boolean reminder, boolean repeatWeekly) {
        service.saveEvent(new PersonalEvent(id, title, date, start, end, venue, note, reminder, repeatWeekly));
    }
    public void deleteEvent(String id) { service.deleteEvent(id); }
    public void setReminder(String id, boolean enabled) { service.setReminderEnabled(id, enabled); }
    public void setReminderOffset(ReminderOffset offset) { service.setReminderOffset(offset); }
    public ReminderOffset getReminderOffset() { return service.getReminderOffset(); }
    public boolean isReminderEnabled(String id) { return service.isReminderEnabled(id); }
    public void snooze(String id) { reminders.snooze(id); }
    public void export(List<ClassSession> sessions, Path path) throws Exception { exporter.export(sessions, path); }
    public String getStatus(ClassSession session, LocalDateTime now) {
        DayOfWeek today = now.getDayOfWeek();
        if (session.getDay().getValue() < today.getValue()) return "Completed";
        if (session.getDay() != today) return "";
        if (!now.toLocalTime().isBefore(session.getStart()) && now.toLocalTime().isBefore(session.getEnd())) return "Ongoing";
        if (!now.toLocalTime().isBefore(session.getEnd())) return "Completed";
        ClassSession next = service.getSessions(today).stream()
                .filter(s -> s.getStart().isAfter(now.toLocalTime()))
                .min(java.util.Comparator.comparing(ClassSession::getStart)).orElse(null);
        return next != null && next.getId().equals(session.getId()) ? "Up next" : "";
    }
    public String getCountdown(ClassSession session, String status, LocalDateTime now) {
        long minutes;
        if (status.equals("Up next")) {
            minutes = Math.max(0, Duration.between(now, LocalDateTime.of(now.toLocalDate(), session.getStart())).toMinutes());
            return minutes < 60 ? "Starts in " + minutes + " min"
                    : "Starts in " + (minutes / 60) + " hr " + (minutes % 60) + " min";
        }
        if (status.equals("Ongoing")) {
            minutes = Math.max(0, Duration.between(now, LocalDateTime.of(now.toLocalDate(), session.getEnd())).toMinutes());
            return "Ends in " + minutes + " min";
        }
        return "";
    }
    public String getStatus(ClassSession session, LocalDate date, LocalDateTime now) {
        if (date.isBefore(now.toLocalDate())) return "Completed";
        if (date.isAfter(now.toLocalDate())) return "";
        if (!now.toLocalTime().isBefore(session.getStart()) && now.toLocalTime().isBefore(session.getEnd())) return "Ongoing";
        if (!now.toLocalTime().isBefore(session.getEnd())) return "Completed";
        ClassSession next = service.getSessionsForDate(date).stream()
                .filter(s -> s.getStart().isAfter(now.toLocalTime()))
                .min(java.util.Comparator.comparing(ClassSession::getStart)).orElse(null);
        return next != null && next.getId().equals(session.getId()) ? "Up next" : "";
    }
    public String getCountdown(ClassSession session, String status, LocalDate date, LocalDateTime now) {
        if (status.equals("Up next")) {
            long minutes = Math.max(0, Duration.between(now, LocalDateTime.of(date, session.getStart())).toMinutes());
            return minutes < 60 ? "in " + minutes + " min"
                    : "in " + (minutes / 60) + " hr " + (minutes % 60) + " min";
        }
        if (status.equals("Ongoing")) {
            long minutes = Math.max(0, Duration.between(now, LocalDateTime.of(date, session.getEnd())).toMinutes());
            return "Ends in " + minutes + " min";
        }
        return "";
    }
    public PersonalEvent previewEvent(String id, String title, DayOfWeek day, java.time.LocalTime start,
                                      java.time.LocalTime end, String venue, String note, boolean reminder) {
        return new PersonalEvent(id, title, day, start, end, venue, note, reminder);
    }
    public PersonalEvent previewEvent(String id, String title, LocalDate date, java.time.LocalTime start,
                                      java.time.LocalTime end, String venue, String note, boolean reminder,
                                      boolean repeatWeekly) {
        return new PersonalEvent(id, title, date, start, end, venue, note, reminder, repeatWeekly);
    }
    public PersonalEvent newEvent(String title, DayOfWeek day, java.time.LocalTime start,
                                  java.time.LocalTime end, String venue, String note, boolean reminder) {
        return new PersonalEvent(UUID.randomUUID().toString(), title, day, start, end, venue, note, reminder);
    }
    public PersonalEvent newEvent(String title, LocalDate date, java.time.LocalTime start, java.time.LocalTime end,
                                  String venue, String note, boolean reminder, boolean repeatWeekly) {
        return new PersonalEvent(UUID.randomUUID().toString(), title, date, start, end, venue, note, reminder, repeatWeekly);
    }
    public List<ClassSession> findClashesForDate(PersonalEvent event) {
        return service.getSessionsForDate(event.getDate()).stream()
                .filter(session -> !session.getId().equals(event.getId()))
                .filter(session -> event.getStart().isBefore(session.getEnd())
                        && session.getStart().isBefore(event.getEnd()))
                .toList();
    }
}
