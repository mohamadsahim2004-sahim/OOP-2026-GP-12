package com.facultyams.service.timetable;

import com.facultyams.dao.StudentTimetableDAO;
import com.facultyams.dao.StudentTimetableDAO.SlotRow;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.timetable.*;
import com.facultyams.security.StudentAccess;
import com.facultyams.service.StudentServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Timetable for the logged-in student. Academic sessions come from the database (the timetable slots of the
 * courses the student is enrolled in); the student's own personal events and reminder preferences are kept in a
 * per-student properties file so students never see each other's events.
 */
public final class FileTimetableService implements TimetableService {
    private final Path settingsFile;
    private final Properties settings = new Properties();
    private final StudentTimetableDAO timetableDAO = new StudentTimetableDAO();
    private final Map<String, PersonalEvent> events = new LinkedHashMap<>();

    public FileTimetableService() {
        this(Path.of(System.getProperty("user.home"), ".srms",
                "timetable-" + StudentAccess.currentStudentId() + ".properties"));
    }

    public FileTimetableService(Path settingsFile) {
        this.settingsFile = Objects.requireNonNull(settingsFile);
        load();
    }

    @Override public synchronized List<ClassSession> getSessions() {
        List<ClassSession> result = new ArrayList<>(academicSessions());
        result.addAll(events.values());
        result.sort(Comparator.comparing(ClassSession::getDay).thenComparing(ClassSession::getStart));
        return List.copyOf(result);
    }

    @Override public synchronized List<ClassSession> getSessions(DayOfWeek day) {
        return getSessions().stream().filter(s -> s.getDay() == day).toList();
    }

    @Override public synchronized List<ClassSession> getSessionsForDate(LocalDate date) {
        List<ClassSession> result = new ArrayList<>();
        academicSessions().stream().filter(s -> s.getDay() == date.getDayOfWeek()).forEach(result::add);
        events.values().stream().filter(event -> event.isRepeatWeekly()
                ? event.getDay() == date.getDayOfWeek()
                : event.getDate().equals(date)).forEach(result::add);
        result.sort(Comparator.comparing(ClassSession::getStart));
        return List.copyOf(result);
    }

    @Override public synchronized List<ClassSession> search(String query, ClassType type, DayOfWeek day) {
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return getSessions().stream()
                .filter(s -> day == null || s.getDay() == day)
                .filter(s -> type == null || s.getType() == type)
                .filter(s -> term.isEmpty()
                        || s.getCourse().getCode().toLowerCase(Locale.ROOT).contains(term)
                        || s.getDisplayName().toLowerCase(Locale.ROOT).contains(term)
                        || s.getLecturer().getName().toLowerCase(Locale.ROOT).contains(term)
                        || s.getVenue().getName().toLowerCase(Locale.ROOT).contains(term))
                .sorted(Comparator.comparing(ClassSession::getDay).thenComparing(ClassSession::getStart))
                .toList();
    }

    @Override public synchronized List<ClassSession> findClashes(PersonalEvent event) {
        return getSessionsForDate(event.getDate()).stream()
                .filter(s -> !s.getId().equals(event.getId()))
                .filter(s -> event.getStart().isBefore(s.getEnd()) && s.getStart().isBefore(event.getEnd()))
                .toList();
    }

    @Override public synchronized void saveEvent(PersonalEvent event) {
        events.put(event.getId(), event);
        persist();
    }

    @Override public synchronized void deleteEvent(String id) {
        events.remove(id);
        persist();
    }

    @Override public synchronized Optional<PersonalEvent> getEvent(String id) {
        return Optional.ofNullable(events.get(id));
    }

    @Override public synchronized boolean isReminderEnabled(String sessionId) {
        PersonalEvent event = events.get(sessionId);
        if (event != null) return Boolean.parseBoolean(settings.getProperty("reminder.session." + sessionId,
                Boolean.toString(event.isReminderEnabled())));
        return Boolean.parseBoolean(settings.getProperty("reminder.session." + sessionId, "false"));
    }

    @Override public synchronized void setReminderEnabled(String sessionId, boolean enabled) {
        settings.setProperty("reminder.session." + sessionId, Boolean.toString(enabled));
        PersonalEvent event = events.get(sessionId);
        if (event != null) {
            events.put(sessionId, new PersonalEvent(event.getId(), event.getDisplayName(), event.getDate(),
                    event.getStart(), event.getEnd(), event.getVenue().getName(), event.getNote(), enabled,
                    event.isRepeatWeekly()));
        }
        persist();
    }

    @Override public synchronized ReminderOffset getReminderOffset() {
        try {
            return ReminderOffset.valueOf(settings.getProperty("reminder.offset", ReminderOffset.FIFTEEN.name()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid saved reminder offset in " + settingsFile, ex);
        }
    }

    @Override public synchronized void setReminderOffset(ReminderOffset offset) {
        settings.setProperty("reminder.offset", Objects.requireNonNull(offset).name());
        persist();
    }

    private void load() {
        if (!Files.exists(settingsFile)) return;
        try (InputStream input = Files.newInputStream(settingsFile)) {
            settings.load(input);
            int count = Integer.parseInt(settings.getProperty("event.count", "0"));
            for (int i = 0; i < count; i++) {
                String[] p = settings.getProperty("event." + i, "").split("\\|", -1);
                if (p.length != 9) throw new IOException("Malformed saved event at index " + i);
                PersonalEvent event;
                try {
                    event = new PersonalEvent(p[0], decode(p[1]), LocalDate.parse(p[2]),
                            LocalTime.parse(p[3]), LocalTime.parse(p[4]), decode(p[5]), decode(p[6]),
                            Boolean.parseBoolean(p[7]), Boolean.parseBoolean(p[8]));
                } catch (java.time.format.DateTimeParseException legacyFormat) {
                    event = new PersonalEvent(p[0], decode(p[1]), DayOfWeek.valueOf(p[2]),
                            LocalTime.parse(p[3]), LocalTime.parse(p[4]), decode(p[5]), decode(p[6]),
                            Boolean.parseBoolean(p[7]));
                }
                events.put(event.getId(), event);
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Could not load timetable settings from " + settingsFile, ex);
        }
    }

    private void persist() {
        settings.setProperty("event.count", Integer.toString(events.size()));
        int i = 0;
        for (PersonalEvent event : events.values()) {
            settings.setProperty("event." + i++, String.join("|", event.getId(),
                    encode(event.getDisplayName()), event.getDate().toString(), event.getStart().toString(),
                    event.getEnd().toString(), encode(event.getVenue().getName()), encode(event.getNote()),
                    Boolean.toString(event.isReminderEnabled()), Boolean.toString(event.isRepeatWeekly())));
        }
        try {
            Files.createDirectories(settingsFile.getParent());
            try (OutputStream output = Files.newOutputStream(settingsFile)) {
                settings.store(output, "SRMS timetable preferences");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save timetable settings to " + settingsFile, ex);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8);
    }

    /** The weekly slots of the student's enrolled courses, read fresh from the database. */
    private List<ClassSession> academicSessions() {
        int me = StudentAccess.currentStudentId();
        List<SlotRow> rows;
        try {
            rows = timetableDAO.findForStudent(me);
        } catch (DatabaseException e) {
            throw new StudentServiceException("Your timetable could not be loaded. Please try again.", e);
        }
        List<ClassSession> sessions = new ArrayList<>();
        for (SlotRow r : rows) {
            if (!r.end().isAfter(r.start())) continue;   // ignore malformed slots instead of crashing the screen
            DayOfWeek day = DayOfWeek.valueOf(r.day());
            String lecturer = r.lecturer() == null || r.lecturer().isBlank() ? "Not assigned" : r.lecturer();
            Course course = new Course(r.code(), r.name());
            String id = "tt-" + r.timetableId();
            if ("PRACTICAL".equals(r.courseType())) {
                sessions.add(new Practical(id, day, r.start(), r.end(), course, new Lecturer(lecturer), new Venue(r.venue())));
            } else {
                sessions.add(new Lecture(id, day, r.start(), r.end(), course, new Lecturer(lecturer), new Venue(r.venue())));
            }
        }
        return sessions;
    }

    /** Ids of academic sessions that overlap another one on the same day (possible when the timetable is edited later). */
    public Set<String> findAcademicClashIds() {
        List<ClassSession> sessions = academicSessions();
        Set<String> clashing = new HashSet<>();
        for (int i = 0; i < sessions.size(); i++) {
            for (int j = i + 1; j < sessions.size(); j++) {
                ClassSession a = sessions.get(i);
                ClassSession b = sessions.get(j);
                if (a.getDay() == b.getDay() && a.getStart().isBefore(b.getEnd()) && b.getStart().isBefore(a.getEnd())) {
                    clashing.add(a.getId());
                    clashing.add(b.getId());
                }
            }
        }
        return clashing;
    }
}
