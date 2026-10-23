package com.facultyams.service.timetable;

import com.facultyams.model.timetable.*;
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

/** In-memory sample schedule with user preferences and events persisted in a properties file. */
public final class FileTimetableService implements TimetableService {
    private final Path settingsFile;
    private final Properties settings = new Properties();
    private final List<ClassSession> academicSessions = createSampleSessions();
    private final Map<String, PersonalEvent> events = new LinkedHashMap<>();

    public FileTimetableService() {
        this(Path.of(System.getProperty("user.home"), ".srms", "timetable.properties"));
    }

    public FileTimetableService(Path settingsFile) {
        this.settingsFile = Objects.requireNonNull(settingsFile);
        load();
    }

    @Override public synchronized List<ClassSession> getSessions() {
        List<ClassSession> result = new ArrayList<>(academicSessions);
        result.addAll(events.values());
        result.sort(Comparator.comparing(ClassSession::getDay).thenComparing(ClassSession::getStart));
        return List.copyOf(result);
    }

    @Override public synchronized List<ClassSession> getSessions(DayOfWeek day) {
        return getSessions().stream().filter(s -> s.getDay() == day).toList();
    }

    @Override public synchronized List<ClassSession> getSessionsForDate(LocalDate date) {
        List<ClassSession> result = new ArrayList<>();
        academicSessions.stream().filter(s -> s.getDay() == date.getDayOfWeek()).forEach(result::add);
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

    private static List<ClassSession> createSampleSessions() {
        List<ClassSession> sessions = new ArrayList<>();
        addPractical(sessions, "cs301-mon", DayOfWeek.MONDAY, "08:30", "10:30", "CS301", "Data Structures", "Lab 02 (Computer Centre)", "Dr. A. Fernando");
        addLecture(sessions, "cs305-mon", DayOfWeek.MONDAY, "10:45", "12:45", "CS305", "Software Engineering", "Lecture Hall 4A", "Mrs. T. Wickramasinghe", true);
        addPractical(sessions, "cs306-mon", DayOfWeek.MONDAY, "13:30", "15:30", "CS306", "Web Application Development", "Lab 01 (Networking Lab)", "Mr. R. Kumara");
        addLecture(sessions, "cs302-tue", DayOfWeek.TUESDAY, "08:30", "10:30", "CS302", "Database Management Systems", "Lecture Hall 2B", "Prof. S. Perera", true);
        addLecture(sessions, "cs304-tue", DayOfWeek.TUESDAY, "11:00", "13:00", "CS304", "Computer Networks", "Lecture Hall 1A", "Dr. M. Silva", true);
        addPractical(sessions, "cs302lab-tue", DayOfWeek.TUESDAY, "14:00", "16:00", "CS302", "DBMS Lab Practical", "Lab 03 (Database Lab)", "Prof. S. Perera");
        addLecture(sessions, "cs303-wed", DayOfWeek.WEDNESDAY, "08:30", "11:30", "CS303", "Operating Systems", "Lecture Hall 3B", "Dr. N. Rajapakse", true);
        addTutorial(sessions, "cs301-wed", DayOfWeek.WEDNESDAY, "13:30", "15:30", "CS301", "Data Structures Tutorial", "Room 204 (Seminar Block)", "Dr. A. Fernando");
        addPractical(sessions, "cs304-thu", DayOfWeek.THURSDAY, "09:00", "11:00", "CS304", "Computer Networks Lab", "Networking Lab (3rd Floor)", "Dr. M. Silva");
        addTutorial(sessions, "cs305-thu", DayOfWeek.THURSDAY, "11:30", "13:30", "CS305", "Software Engineering Tutorial", "Room 102", "Mrs. T. Wickramasinghe");
        addPractical(sessions, "cs303-fri", DayOfWeek.FRIDAY, "08:30", "10:30", "CS303", "Operating Systems Lab", "Lab 02 (OS Lab)", "Dr. N. Rajapakse");
        addTutorial(sessions, "cs306-fri", DayOfWeek.FRIDAY, "10:45", "12:45", "CS306", "Web Application Dev Tutorial", "Lecture Hall 1B", "Mr. R. Kumara");
        return sessions;
    }

    private static void addLecture(List<ClassSession> l, String id, DayOfWeek d, String s, String e, String c, String n, String v, String lecturer, boolean ignored) {
        l.add(new Lecture(id, d, LocalTime.parse(s), LocalTime.parse(e), new Course(c, n), new Lecturer(lecturer), new Venue(v)));
    }
    private static void addPractical(List<ClassSession> l, String id, DayOfWeek d, String s, String e, String c, String n, String v, String lecturer) {
        l.add(new Practical(id, d, LocalTime.parse(s), LocalTime.parse(e), new Course(c, n), new Lecturer(lecturer), new Venue(v)));
    }
    private static void addTutorial(List<ClassSession> l, String id, DayOfWeek d, String s, String e, String c, String n, String v, String lecturer) {
        l.add(new Tutorial(id, d, LocalTime.parse(s), LocalTime.parse(e), new Course(c, n), new Lecturer(lecturer), new Venue(v)));
    }
}
