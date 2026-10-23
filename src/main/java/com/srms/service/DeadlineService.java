package com.srms.service;

import com.srms.model.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Reads upcoming submission deadlines from course activities. */
public class DeadlineService {
    private final CourseService courseService;

    public DeadlineService(CourseService courseService) {
        this.courseService = Objects.requireNonNull(courseService);
    }

    public List<Deadline> getUpcomingDeadlines(int limit) {
        if (limit <= 0) return List.of();

        LocalDate today = LocalDate.now();
        List<Deadline> deadlines = new ArrayList<>();
        for (Course course : courseService.findCourses("ST1001")) {
            for (Section section : courseService.sections(course.id())) {
                for (Activity activity : courseService.activities(section.id())) {
                    if (activity instanceof Assignment assignment && assignment.dueAt() != null) {
                        LocalDate dueDate = assignment.dueAt().toLocalDate();
                        if (!dueDate.isBefore(today)) {
                            DeadlineType type = assignment.title().toLowerCase().contains("project")
                                    ? DeadlineType.PROJECT : DeadlineType.ASSIGNMENT;
                            deadlines.add(new Deadline(
                                    assignment.id(), course.code(), course.title(), assignment.title(),
                                    type, dueDate, DeadlineTarget.SUBMISSION));
                        }
                    }
                }
            }
        }
        deadlines.sort(Comparator.comparing(Deadline::dueDate).thenComparing(Deadline::title));
        return List.copyOf(deadlines.subList(0, Math.min(limit, deadlines.size())));
    }
}
