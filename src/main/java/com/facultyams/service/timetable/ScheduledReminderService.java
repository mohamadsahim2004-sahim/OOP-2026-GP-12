package com.facultyams.service.timetable;

import com.facultyams.model.timetable.*;
import java.time.*;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Daemon scheduler that fires each enabled class reminder once per date. */
public final class ScheduledReminderService implements ReminderService {
    private final TimetableService timetable;
    private final ScheduledExecutorService executor;
    private final CopyOnWriteArrayList<ReminderListener> listeners = new CopyOnWriteArrayList<>();
    private final Set<String> fired = ConcurrentHashMap.newKeySet();

    public ScheduledReminderService(TimetableService timetable) {
        this.timetable = timetable;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "srms-reminders");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(this::checkReminders, 0, 20, TimeUnit.SECONDS);
    }

    @Override public void addListener(ReminderListener listener) { listeners.add(listener); }

    private void checkReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            for (ClassSession session : timetable.getSessionsForDate(now.toLocalDate())) {
                if (!timetable.isReminderEnabled(session.getId())) continue;
                LocalDateTime start = LocalDateTime.of(now.toLocalDate(), session.getStart());
                long minutes = Duration.between(now, start).toMinutes();
                String key = session.getId() + ":" + now.toLocalDate();
                if (minutes >= 0 && minutes <= timetable.getReminderOffset().getMinutes() && fired.add(key)) {
                    notifyListeners(session, start);
                }
            }
        } catch (RuntimeException ex) {
            System.err.println("Timetable reminder check failed: " + ex.getMessage());
        }
    }

    private void notifyListeners(ClassSession session, LocalDateTime start) {
        Reminder reminder = new Reminder(session.getId(), session.getCourse().getCode() + " " + session.getDisplayName()
                + " starts in " + Math.max(0, Duration.between(LocalDateTime.now(), start).toMinutes())
                + " min at " + session.getVenue().getName(), LocalDateTime.now(), false);
        listeners.forEach(listener -> listener.onReminder(reminder));
    }

    @Override public void snooze(String sessionId) {
        executor.schedule(() -> timetable.getSessions().stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .ifPresent(s -> listeners.forEach(l -> l.onReminder(new Reminder(s.getId(),
                        s.getCourse().getCode() + " " + s.getDisplayName() + " reminder (snoozed 5 min)",
                        LocalDateTime.now(), false)))), 5, TimeUnit.MINUTES);
    }

    @Override public void close() { executor.shutdownNow(); }
}
