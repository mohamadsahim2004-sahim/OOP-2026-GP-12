package com.facultyams.service;

import com.facultyams.dao.CourseDAO;
import com.facultyams.dao.TimeTableDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.TimeTable;
import com.facultyams.security.Role;
import com.facultyams.security.Session;
import com.facultyams.util.ValidationUtil;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Timetable rules. Adding, editing and deleting slots is Admin-only; reading is open to every logged-in
 * user so students (their own timetable) and technical officers (department timetables) keep working.
 * The permission check always runs first, before any argument is looked at.
 */
public class TimeTableService {

    public static final int MAX_VENUE_LENGTH = 50;      // timetables.venue is VARCHAR(50)

    private final TimeTableDAO timeTableDAO;
    private final CourseDAO courseDAO;

    public TimeTableService() {
        this(new TimeTableDAO(), new CourseDAO());
    }

    public TimeTableService(TimeTableDAO timeTableDAO, CourseDAO courseDAO) {
        this.timeTableDAO = timeTableDAO;
        this.courseDAO = courseDAO;
    }

    // ----- reading: any logged-in user -----

    /** Slots in weekday/time order. Department and semester (those of the slot's course) may each be null = all. */
    public List<TimeTable> getTimeTable(Integer departmentId, Integer semester)
            throws AuthorizationException, DatabaseException {
        Session.requireLogin();
        return timeTableDAO.findAll(departmentId, semester);
    }

    /**
     * Like {@link #getTimeTable}, narrowed by a keyword that matches course code, course name, venue or
     * weekday. A blank keyword means no keyword filter.
     */
    public List<TimeTable> searchTimeTable(String keyword, Integer departmentId, Integer semester)
            throws AuthorizationException, DatabaseException {
        Session.requireLogin();
        return timeTableDAO.search(keyword, departmentId, semester);
    }

    public TimeTable getSlotById(int timetableId) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireLogin();
        ValidationUtil.requirePositiveId(timetableId, "timetable id");
        TimeTable slot = timeTableDAO.findById(timetableId);
        if (slot == null) {
            throw new ValidationException("Timetable slot with id " + timetableId + " was not found.");
        }
        return slot;
    }

    // ----- changing: Admin only -----

    /** Validates and saves a new slot. Returns the saved slot (with its new id). */
    public TimeTable addSlot(TimeTable slot) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        if (slot == null) {
            throw new ValidationException("Timetable details are required.");
        }
        slot.setTimetableId(0);
        validate(slot);
        timeTableDAO.insert(slot);
        return slot;
    }

    public void updateSlot(TimeTable slot) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        if (slot == null) {
            throw new ValidationException("Timetable details are required.");
        }
        getSlotById(slot.getTimetableId());
        validate(slot);
        timeTableDAO.update(slot);
    }

    public void deleteSlot(int timetableId) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        getSlotById(timetableId);
        timeTableDAO.delete(timetableId);
    }

    /**
     * Checks every field (cheap checks first, then the database ones) and stores the cleaned values
     * back into the slot: times cut to whole minutes, venue trimmed.
     */
    private void validate(TimeTable slot) throws ValidationException, DatabaseException {
        DayOfWeek day = slot.getDayOfWeek();
        if (day == null || day == DayOfWeek.SUNDAY) {
            throw new ValidationException("Day must be Monday to Saturday.");
        }
        if (slot.getStartTime() == null || slot.getEndTime() == null) {
            throw new ValidationException("Start time and end time are required.");
        }
        LocalTime start = slot.getStartTime().truncatedTo(ChronoUnit.MINUTES);
        LocalTime end = slot.getEndTime().truncatedTo(ChronoUnit.MINUTES);
        if (!end.isAfter(start)) {
            throw new ValidationException("End time must be after start time.");
        }
        String venue = ValidationUtil.requireNotBlank(slot.getVenue(), "Lecture hall / venue");
        ValidationUtil.requireMaxLength(venue, MAX_VENUE_LENGTH, "Lecture hall / venue");

        ValidationUtil.requirePositiveId(slot.getCourseId(), "course");
        if (courseDAO.findById(slot.getCourseId()) == null) {
            throw new ValidationException("Please select an existing course.");
        }

        List<TimeTable> clashes = timeTableDAO.findConflicts(
                day, start, end, venue, slot.getCourseId(), slot.getTimetableId());
        if (!clashes.isEmpty()) {
            TimeTable other = clashes.get(0);
            if (other.getCourseId() == slot.getCourseId()) {
                throw new ValidationException("This course already has a lecture at that time: " + other + ".");
            }
            throw new ValidationException("The venue is already booked at that time: " + other + ".");
        }

        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setVenue(venue);
    }
}