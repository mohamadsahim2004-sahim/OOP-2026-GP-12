package com.fot.ams;

import com.fot.ams.ui.LoginFrame;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Renders the LoginFrame to an image file for inspection and artifact documentation.
 */
public class UISnapshotUtility {
    public static void main(String[] args) throws Exception {
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeAndWait(() -> {
            try {
                LoginFrame frame = new LoginFrame();
                frame.setSize(960, 620);
                frame.getContentPane().setBounds(0, 0, 960, 620);
                layoutRecursively(frame.getContentPane());

                int w = 960;
                int h = 620;

                BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = image.createGraphics();
                g2.setColor(new Color(241, 243, 249));
                g2.fillRect(0, 0, w, h);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                frame.getContentPane().paint(g2);
                g2.dispose();

                String artifactDir = "C:/Users/MI COMPUTERS/.gemini/antigravity-ide/brain/42eb861e-3fb8-4aa5-a84d-4d0af7f1cb26";
                File outFile = new File(artifactDir, "login_preview.png");
                ImageIO.write(image, "png", outFile);
                System.out.println("Snapshot successfully written to: " + outFile.getAbsolutePath());

                File localOut = new File("login_preview.png");
                ImageIO.write(image, "png", localOut);
                System.out.println("Local preview saved: " + localOut.getAbsolutePath());

                frame.dispose();
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
