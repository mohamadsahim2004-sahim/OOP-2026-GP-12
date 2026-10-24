package com.facultyams.ui.admin;

public class TimeTableFormPanel {
    
}
package com.facultyams.ui.admin;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Course;
import com.facultyams.model.TimeTable;
import com.facultyams.service.CourseService;
import com.facultyams.service.TimeTableService;
import com.facultyams.util.UIUtil;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;

/** Add / Edit timetable slot form, shown in a dialog. {@link TimeTableService} checks the times and clashes. */
final class TimeTableFormPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DayOfWeek[] DAYS = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};

    private final transient TimeTableService timeTableService;
    /** null when adding. */
    private final transient TimeTable existing;

    final JComboBox<Course> courseCombo = new JComboBox<>();
    final JComboBox<DayOfWeek> dayCombo = new JComboBox<>(DAYS);
    final JTextField startField = new JTextField("08:00", 8);
    final JTextField endField = new JTextField("10:00", 8);
    final JTextField venueField = new JTextField(16);

    TimeTableFormPanel(TimeTableService timeTableService, CourseService courseService, TimeTable existing)
            throws DatabaseException, AuthorizationException {
        super(new BorderLayout());
        this.timeTableService = timeTableService;
        this.existing = existing;
        setOpaque(false);

        for (Course c : courseService.getAllCourses()) {
            courseCombo.addItem(c);
        }
        dayCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, value instanceof DayOfWeek d ? dayName(d) : "",
                        index, isSelected, cellHasFocus);
            }
        });

        JPanel form = Forms.grid();
        Forms.add(form, 0, 0, 2, "Course *", courseCombo);
        Forms.add(form, 0, 1, 1, "Day *", dayCombo);
        Forms.add(form, 1, 1, 1, "Lecture hall / venue *", venueField);
        Forms.add(form, 0, 2, 1, "Start (HH:mm) *", startField);
        Forms.add(form, 1, 2, 1, "End (HH:mm) *", endField);
        add(form, BorderLayout.CENTER);

        if (existing != null) {
            for (int i = 0; i < courseCombo.getItemCount(); i++) {
                if (courseCombo.getItemAt(i).getCourseId() == existing.getCourseId()) {
                    courseCombo.setSelectedIndex(i);
                }
            }
            dayCombo.setSelectedItem(existing.getDayOfWeek());
            startField.setText(existing.getStartTime().toString());
            endField.setText(existing.getEndTime().toString());
            venueField.setText(existing.getVenue());
        }
    }

    static String dayName(DayOfWeek day) {
        return day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    /** Saves through the service; returns false (after showing the message) if it was refused. */
    boolean save() {
        try {
            Course course = (Course) courseCombo.getSelectedItem();
            if (course == null) {
                throw new ValidationException("Please select a course.");
            }
            TimeTable t = new TimeTable();
            t.setCourseId(course.getCourseId());
            t.setDayOfWeek((DayOfWeek) dayCombo.getSelectedItem());
            try {
                t.setStartTime(LocalTime.parse(startField.getText().trim()));
                t.setEndTime(LocalTime.parse(endField.getText().trim()));
            } catch (DateTimeParseException e) {
                throw new ValidationException("Times must be written as HH:mm, e.g. 08:00.", e);
            }
            t.setVenue(venueField.getText());
            if (existing == null) {
                timeTableService.addSlot(t);
            } else {
                t.setTimetableId(existing.getTimetableId());
                timeTableService.updateSlot(t);
            }
            return true;
        } catch (ValidationException | AuthorizationException | DatabaseException e) {
            UIUtil.showException(this, e);
            return false;
        }
    }

    static boolean showDialog(Component parent, TimeTableService timeTableService, CourseService courseService,
                              TimeTable existing) {
        TimeTableFormPanel form;
        try {
            form = new TimeTableFormPanel(timeTableService, courseService, existing);
        } catch (DatabaseException | AuthorizationException e) {
            UIUtil.showException(parent, e);
            return false;
        }
        return FormDialog.show(parent, existing == null ? "Add Timetable Slot" : "Edit Timetable Slot", form,
                existing == null ? "Add slot" : "Save changes", form::save);
    }
}