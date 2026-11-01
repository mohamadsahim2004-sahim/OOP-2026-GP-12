package com.facultyams;

import com.facultyams.dao.UserDAO;
import com.facultyams.model.Student;
import com.facultyams.security.Session;
import com.facultyams.ui.UndergraduateDashboardFrame;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Opens the portal as a student, visits every screen and saves a screenshot of each (needs a display, e.g. Xvfb). */
public final class StudentUiSmoke {
    private StudentUiSmoke() { }

    public static void main(String[] args) throws Exception {
        int userId = Integer.parseInt(args[0]);
        File outDir = new File(args[1]);
        outDir.mkdirs();
        List<Throwable> errors = new ArrayList<>();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> { errors.add(e); e.printStackTrace(); });
        Session.start(new UserDAO().findById(userId));
        final UndergraduateDashboardFrame[] frame = new UndergraduateDashboardFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            frame[0] = new UndergraduateDashboardFrame((Student) Session.getCurrentUser());
            frame[0].setVisible(true);
        });
        String[] views = {"Dashboard", "Profile", "Course Enrollment", "Course Materials", "Results",
                "Attendance", "Medical Records", "Timetable", "Notice Board"};
        for (String view : views) {
            SwingUtilities.invokeAndWait(() -> frame[0].navigateTo(view));
            Thread.sleep(1800);
            shot(frame[0], new File(outDir, "u" + userId + "-" + view.replace(' ', '_') + ".png"));
            System.out.println("visited " + view);
        }
        SwingUtilities.invokeAndWait(() -> frame[0].navigateTo("Results"));
        Thread.sleep(1500);
        System.out.println(errors.isEmpty() ? "NO UNCAUGHT EXCEPTIONS" : "UNCAUGHT EXCEPTIONS: " + errors.size());
        System.exit(errors.isEmpty() ? 0 : 1);
    }

    private static void shot(UndergraduateDashboardFrame f, File file) throws Exception {
        BufferedImage img = new BufferedImage(f.getWidth(), f.getHeight(), BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> f.paint(img.getGraphics()));
        ImageIO.write(img, "png", file);
    }
}
