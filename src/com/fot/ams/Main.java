package com.fot.ams;

import com.fot.ams.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.util.Map;

/**
 * Entry point for the Faculty of Technology Academic Management System.
 * University of Ruhuna - Course Unit: ICT2132 Object Oriented Programming Practicum.
 */
public class Main {
    public static void main(String[] args) {
        // Enable subpixel text rendering and anti-aliasing on Windows
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");

        try {
            // Use System Look and Feel for native text rendering & window decorations
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
