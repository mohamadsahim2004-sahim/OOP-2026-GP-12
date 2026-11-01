package com.facultyams.service.timetable;

import com.facultyams.model.timetable.ClassSession;
import java.nio.file.Path;
import java.util.List;

/** Exports schedule data to a user-selected destination. */
@FunctionalInterface
public interface Exportable {
    void export(List<ClassSession> sessions, Path destination) throws Exception;
}
