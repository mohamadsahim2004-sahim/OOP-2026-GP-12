package com.facultyams.service;

import java.time.LocalDateTime;
import java.util.*;

import com.facultyams.model.*;

/**
 * Mock implementation of CourseService delivering realistic university courses,
 * weekly modules, resources, and assignments.
 */
public class MockCourseService implements CourseService {
    private final List<Course> courses = new ArrayList<>();
    private final Map<String, List<Section>> sectionsByCourse = new LinkedHashMap<>();
    private final Map<String, List<Activity>> activitiesBySection = new LinkedHashMap<>();

    public MockCourseService() {
        seedData();
    }

    private void seedData() {
        // Course catalog
        Course c1 = new Course("c1", "ENG2112_ICT", "English III (ICT)", "2025/2026 Semester I", "pink_seed", 75, null, 3, "Dr. Perera");
        Course c2 = new Course("c2", "ICT2113", "Data Structures and Algorithms", "2025/2026 Semester I", "dsa_seed", 42, null, 3, "Dr. Fernando");
        Course c3 = new Course("c3", "ICT2122", "Object Oriented Programming", "2025/2026 Semester I", "oop_seed", 60, null, 3, "Prof. Silva");
        Course c4 = new Course("c4", "ICT2132", "Object Oriented Programming Practicum", "2025/2026 Semester I", "practicum_seed", 30, null, 2, "Mr. Kumara");
        Course c5 = new Course("c5", "ICT2142", "E-Business Systems", "2025/2026 Semester I", "ebiz_seed", 85, null, 3, "Dr. Mendis");
        Course c6 = new Course("c6", "ICT2152", "Object Oriented Analysis and Design", "2025/2026 Semester I", "ooad_seed", 50, null, 3, "Dr. Rajapakse");

        courses.addAll(List.of(c1, c2, c3, c4, c5, c6));

        // Sections for ICT2113 (Screen B mockup)
        Section sGen = new Section("s_gen", "c2", "General", 0);
        Section sW1 = new Section("s_w1", "c2", "Week 01", 1);
        Section sW2 = new Section("s_w2", "c2", "Week 2", 2);
        Section sW3 = new Section("s_w3", "c2", "Week 3", 3);
        Section sW4 = new Section("s_w4", "c2", "Week 4", 4);

        sectionsByCourse.put("c2", List.of(sGen, sW1, sW2, sW3, sW4));

        // Activities for General
        activitiesBySection.put("s_gen", List.of(
                new Announcement("act_ann1", "Announcements")
        ));

        // Activities for Week 01
        LocalDateTime openDate = LocalDateTime.now().minusDays(5).withHour(11).withMinute(0);
        LocalDateTime dueDate = LocalDateTime.now().plusDays(3).withHour(13).withMinute(0);

        activitiesBySection.put("s_w1", List.of(
                new Resource("act_r1", "Introduction 2026", "PDF", "docs/intro.pdf"),
                new Resource("act_r2", "Lecture1_DSA 2026", "PDF", "docs/lecture1.pdf"),
                new Resource("act_r3", "Lab1_DSA Intro", "PDF", "docs/lab1.pdf"),
                new Assignment("as1", "Lab1_DSA Intro Submission Link",
                        "Please submit your completed Lab 1 source code (.java) and documentation (.pdf).\n" +
                                "Ensure your code follows standard Java naming conventions. Maximum 3 files.",
                        openDate, dueDate, List.of("pdf", "docx", "zip", "java"), 20, 3, true)
        ));

        // Activities for Week 2
        activitiesBySection.put("s_w2", List.of(
                new Resource("act_r4", "Stacks", "PDF", "docs/stacks.pdf"),
                new Resource("act_r5", "Lab 2", "PDF", "docs/lab2.pdf"),
                new Assignment("as2", "Lab 2: Stack & Polish Notation",
                        "Implement Stack applications for infix-to-postfix expression conversion.",
                        LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7),
                        List.of("java", "zip", "pdf"), 20, 2, false)
        ));

        // Activities for Week 3
        activitiesBySection.put("s_w3", List.of(
                new Resource("act_r6", "Queues and Circular Buffers", "PDF", "docs/queues.pdf"),
                new Resource("act_r7", "Lab 3 Specifications", "DOCX", "docs/lab3.docx")
        ));

        // Activities for Week 4
        activitiesBySection.put("s_w4", List.of(
                new Resource("act_r8", "Binary Search Trees", "PPTX", "docs/trees.pptx"),
                new Resource("act_r9", "Starter Code Archive", "ZIP", "docs/starter.zip")
        ));

        // Default sections for other courses
        for (Course c : courses) {
            if (!c.id().equals("c2")) {
                Section sec1 = new Section("sec_" + c.id() + "_gen", c.id(), "General", 0);
                Section sec2 = new Section("sec_" + c.id() + "_w1", c.id(), "Week 01", 1);
                sectionsByCourse.put(c.id(), List.of(sec1, sec2));
                activitiesBySection.put(sec1.id(), List.of(new Announcement("a_" + c.id(), "Announcements")));
                activitiesBySection.put(sec2.id(), List.of(
                        new Resource("r_" + c.id(), "Course Syllabus", "PDF", "docs/syllabus.pdf"),
                        new Assignment("as_" + c.id(), "Module Assignment 1",
                                "Comprehensive assignment for module " + c.code() + ".",
                                LocalDateTime.now().minusDays(2), LocalDateTime.now().plusDays(5),
                                List.of("pdf", "zip"), 10, 2, true)
                ));
            }
        }
    }

    @Override
    public List<Course> findCourses(String studentId) {
        return Collections.unmodifiableList(courses);
    }

    @Override
    public List<Section> sections(String courseId) {
        return sectionsByCourse.getOrDefault(courseId, Collections.emptyList());
    }

    @Override
    public List<Activity> activities(String sectionId) {
        return activitiesBySection.getOrDefault(sectionId, Collections.emptyList());
    }
}
