package com.facultyams.ui.admin;

import com.facultyams.dao.CourseDAO;
import com.facultyams.dao.DepartmentDAO;
import com.facultyams.dao.UserDAO;
import com.facultyams.model.Admin;
import com.facultyams.model.Course;
import com.facultyams.model.Department;
import com.facultyams.security.Session;
import com.facultyams.service.CourseService;
import com.facultyams.service.DepartmentService;
import com.facultyams.service.TimeTableService;
import com.facultyams.service.UserService;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Regression check for "the text I type is cut in half": in every Add / Edit dialog each single-line input must be
 * at least as tall as its text plus its border, after the theme has been applied (as FormDialog does).
 *
 * <pre>
 *   java -Djava.awt.headless=true -cp target/classes:target/test-classes com.facultyams.ui.admin.FormFieldChecks
 * </pre>
 * Needs no database (fake DAOs); run with FOTAMS_DB_USER=checks set, like the other checks.
 */
public final class FormFieldChecks {

    private static int passed;
    private static int failed;

    private FormFieldChecks() {
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + name);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
    }

    private static void layoutAll(Component c) {
        if (c instanceof Container ct) {
            ct.doLayout();
            for (Component child : ct.getComponents()) {
                layoutAll(child);
            }
        }
    }

    private static void inputs(Component c, List<JComponent> out) {
        if (c instanceof JComboBox<?> || (c instanceof JTextComponent && !(c instanceof JTextArea))) {
            out.add((JComponent) c);
        }
        if (c instanceof Container ct && !(c instanceof JComboBox<?>)) {
            for (Component child : ct.getComponents()) {
                inputs(child, out);
            }
        }
    }

    /** Builds the form exactly as FormDialog presents it (theme applied, laid out at its preferred size). */
    private static void checkForm(String name, Supplier<JPanel> factory) throws Exception {
        System.out.println("\n[" + name + "]");
        List<String> clipped = new ArrayList<>();
        int[] count = {0};
        SwingUtilities.invokeAndWait(() -> {
            JPanel form = factory.get();
            JPanel content = new JPanel(new BorderLayout());
            content.add(form, BorderLayout.CENTER);
            AdminTheme.apply(content);
            Dimension size = content.getPreferredSize();
            content.setSize(size);
            layoutAll(content);
            layoutAll(content);
            List<JComponent> fields = new ArrayList<>();
            inputs(content, fields);
            for (JComponent f : fields) {
                count[0]++;
                FontMetrics fm = f.getFontMetrics(f.getFont());
                Insets in = f.getInsets();
                int needed = fm.getHeight() + in.top + in.bottom;
                if (f.getHeight() < needed) {
                    String label = f.getAccessibleContext().getAccessibleName();
                    clipped.add((label == null ? f.getClass().getSimpleName() : label) + " is " + f.getHeight()
                            + "px high but its text needs " + needed + "px");
                }
            }
        });
        check(count[0] + " single-line inputs found", count[0] >= 2);
        check("no input is shorter than its text (" + (clipped.isEmpty() ? "all fit" : String.join("; ", clipped)) + ")",
                clipped.isEmpty());
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Admin admin = new Admin();
        admin.setUserId(1);
        admin.setUsername("admin");
        admin.setFullName("Ada Admin");
        admin.setActive(true);
        Session.start(admin);

        DepartmentDAO departments = new DepartmentDAO() {
            @Override
            public List<Department> findAll() {
                List<Department> list = new ArrayList<>();
                Department d = new Department();
                d.setDepartmentId(1);
                d.setDepartmentCode("DICT");
                d.setDepartmentName("Department of ICT");
                list.add(d);
                return list;
            }
        };
        CourseDAO courses = new CourseDAO() {
            @Override
            public List<Course> findAll() {
                List<Course> list = new ArrayList<>();
                Course c = new Course();
                c.setCourseId(1);
                c.setCourseCode("ENG2122");
                c.setCourseName("English III");
                list.add(c);
                return list;
            }
        };
        UserDAO users = new UserDAO() {
            @Override
            public List<com.facultyams.model.User> findByRole(com.facultyams.security.Role role) {
                return new ArrayList<>();
            }
        };
        DepartmentService departmentService = new DepartmentService(departments, users);
        UserService userService = new UserService(users, departments);
        CourseService courseService = new CourseService(courses, departments, users);
        TimeTableService timeTableService = new TimeTableService(null, courses);

        checkForm("Add Notice", () -> new NoticeFormPanel(null, null));
        checkForm("Add User", () -> {
            try {
                return new UserFormPanel(userService, departmentService, null);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        checkForm("Add Timetable Slot", () -> {
            try {
                return new TimeTableFormPanel(timeTableService, courseService, null);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        checkForm("Add Course", () -> {
            try {
                return new CourseFormPanel(courseService, departmentService, userService, null);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        checkForm("Add Department", () -> {
            try {
                return new DepartmentFormPanel(departmentService, userService, null, null, true);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        Session.end();
        System.out.println("\nRESULT: " + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }
}
