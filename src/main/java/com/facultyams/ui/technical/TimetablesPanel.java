package com.facultyams.ui.technical;

import com.facultyams.dao.TimeTableDAO;
import com.facultyams.model.TimeTable;
import com.facultyams.security.Session;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

public class TimetablesPanel extends TechnicalPage {

    private static final long serialVersionUID = 1L;

    private final ReadOnlyTableModel model = ReadOnlyTableModel.ofStrings(
            "Day", "Time", "Course", "Course name", "Venue", "Semester");
    private final JTable table = table(model);

    public TimetablesPanel() {
        add(statusArea(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Integer departmentId = Session.getCurrentUser().getDepartmentId();
        if (departmentId == null) {
            model.setRows(new ArrayList<>());
            showError("Your account has no department, so no timetable can be shown.");
            return;
        }
        load("Loading timetables...", () -> new TimeTableDAO().findAll(departmentId, null), list -> {
            List<Object[]> data = new ArrayList<>();
            for (TimeTable t : list) {
                data.add(new Object[] {t.getDayName(), t.getTimeRange(), t.getCourseCode(),
                        t.getCourseName(), t.getVenue(), String.valueOf(t.getSemester())});
            }
            model.setRows(data);
            showInfo(list.isEmpty() ? "No timetable slots for your department." : list.size() + " timetable slots.");
        });
    }
}