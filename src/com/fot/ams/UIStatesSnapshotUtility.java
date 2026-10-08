package com.fot.ams;

import com.fot.ams.dao.UserDAOImpl;
import com.fot.ams.model.User;
import com.fot.ams.service.SessionManager;
import com.fot.ams.ui.LoginFrame;
import com.fot.ams.ui.dashboard.RoleDashboardPreviewDialog;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

public class UIStatesSnapshotUtility {
    public static void main(String[] args) throws Exception {
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeAndWait(() -> {
            try {
                String artifactDir = "C:/Users/MI COMPUTERS/.gemini/antigravity-ide/brain/42eb861e-3fb8-4aa5-a84d-4d0af7f1cb26";

                // 1. Snapshot: Login with Error Alert Banner
                LoginFrame frame = new LoginFrame();
                frame.setSize(960, 620);
                frame.getContentPane().setBounds(0, 0, 960, 620);

                // Trigger invalid credentials error programmatically
                java.lang.reflect.Method showErrorMethod = LoginFrame.class.getDeclaredMethod("showError", String.class);
                showErrorMethod.setAccessible(true);
                showErrorMethod.invoke(frame, "Invalid username or password. Please try again.");

                layoutRecursively(frame.getContentPane());

                BufferedImage errImg = new BufferedImage(960, 620, BufferedImage.TYPE_INT_RGB);
                Graphics2D gErr = errImg.createGraphics();
                gErr.setColor(new Color(241, 243, 249));
                gErr.fillRect(0, 0, 960, 620);
                gErr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gErr.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                frame.getContentPane().paint(gErr);
                gErr.dispose();

                File errFile = new File(artifactDir, "login_error_preview.png");
                ImageIO.write(errImg, "png", errFile);
                System.out.println("Error preview saved to: " + errFile.getAbsolutePath());

                // 2. Snapshot: Student Dashboard Preview Dialog
                UserDAOImpl dao = new UserDAOImpl();
                User student = dao.findByUsername("TG/2024/2105");
                SessionManager.getInstance().startSession(student);

                RoleDashboardPreviewDialog dialog = new RoleDashboardPreviewDialog(frame, student);
                dialog.setSize(650, 480);
                dialog.getContentPane().setBounds(0, 0, 650, 480);
                layoutRecursively(dialog.getContentPane());

                BufferedImage dashImg = new BufferedImage(650, 480, BufferedImage.TYPE_INT_RGB);
                Graphics2D gDash = dashImg.createGraphics();
                gDash.setColor(new Color(241, 243, 249));
                gDash.fillRect(0, 0, 650, 480);
                gDash.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gDash.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                dialog.getContentPane().paint(gDash);
                gDash.dispose();

                File dashFile = new File(artifactDir, "dashboard_preview.png");
                ImageIO.write(dashImg, "png", dashFile);
                System.out.println("Dashboard preview saved to: " + dashFile.getAbsolutePath());

                frame.dispose();
                dialog.dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void layoutRecursively(java.awt.Container c) {
        c.doLayout();
        for (java.awt.Component child : c.getComponents()) {
            if (child instanceof java.awt.Container) {
                layoutRecursively((java.awt.Container) child);
            }
        }
    }
}
