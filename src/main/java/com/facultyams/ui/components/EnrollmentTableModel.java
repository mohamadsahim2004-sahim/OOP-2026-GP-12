package com.facultyams.ui.components;

import com.facultyams.model.Course;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** Shared eight-column course table data model. */
public abstract class EnrollmentTableModel extends AbstractTableModel {
    private final String[] columns;
    protected List<Course> courses = List.of();

    protected EnrollmentTableModel(String[] columns) {
        this.columns = columns;
    }

    public void setCourses(List<Course> courses) {
        this.courses = List.copyOf(courses);
        fireTableDataChanged();
    }

    public Course getCourseAt(int row) { return courses.get(row); }
    @Override public int getRowCount() { return courses.size(); }
    @Override public int getColumnCount() { return columns.length; }
    @Override public String getColumnName(int column) { return columns[column]; }
    @Override public boolean isCellEditable(int row, int column) { return false; }
    @Override public Class<?> getColumnClass(int column) { return String.class; }
}
