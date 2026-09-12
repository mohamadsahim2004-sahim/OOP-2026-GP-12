package com.fot.ams;

import com.fot.ams.model.UserRole;
import com.fot.ams.ui.RegistrationFrame;
import com.fot.ams.ui.components.RoleTabSelector;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Captures rendered snapshots of RegistrationFrame for Undergraduate and Lecturer roles.
 */
public class RegistrationSnapshotUtility {
    public static void main(String[] args) throws Exception {
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeAndWait(() -> {
            try {
                String artifactDir = "C:/Users/MI COMPUTERS/.gemini/antigravity-ide/brain/42eb861e-3fb8-4aa5-a84d-4d0af7f1cb26";

                // 1. Undergraduate Registration Snapshot
                RegistrationFrame frame1 = new RegistrationFrame();
                frame1.setSize(1020, 680);
                frame1.getContentPane().setBounds(0, 0, 1020, 680);
                layoutRecursively(frame1.getContentPane());

                BufferedImage img1 = renderComponent(frame1.getContentPane(), 1020, 680);
                File out1 = new File(artifactDir, "registration_preview.png");
                ImageIO.write(img1, "png", out1);
                System.out.println("Undergraduate snapshot saved: " + out1.getAbsolutePath());

                // 2. Lecturer Registration Snapshot
                RegistrationFrame frame2 = new RegistrationFrame();
                frame2.setSize(1020, 680);
                frame2.getContentPane().setBounds(0, 0, 1020, 680);

                // Switch to Lecturer
                RoleTabSelector selector = findRoleTabSelector(frame2.getContentPane());
                if (selector != null) {
                    selector.setSelectedRole(UserRole.LECTURER);
                }
                layoutRecursively(frame2.getContentPane());

                BufferedImage img2 = renderComponent(frame2.getContentPane(), 1020, 680);
                File out2 = new File(artifactDir, "registration_lecturer_preview.png");
                ImageIO.write(img2, "png", out2);
                System.out.println("Lecturer snapshot saved: " + out2.getAbsolutePath());

                frame1.dispose();
                frame2.dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static BufferedImage renderComponent(java.awt.Component comp, int w, int h) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(new Color(241, 243, 249));
        g2.fillRect(0, 0, w, h);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        comp.paint(g2);
        g2.dispose();
        return image;
    }

    private static RoleTabSelector findRoleTabSelector(java.awt.Container c) {
        for (java.awt.Component child : c.getComponents()) {
            if (child instanceof RoleTabSelector) {
                return (RoleTabSelector) child;
            }
            if (child instanceof java.awt.Container) {
                RoleTabSelector found = findRoleTabSelector((java.awt.Container) child);
                if (found != null) return found;
            }
        }
        return null;
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
