package com.facultyams.util;

import com.facultyams.exception.AuthenticationException;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Small Swing helpers shared by all screens so error handling looks the same everywhere.
 */
public final class UIUtil {

    private UIUtil() {
        // utility class
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /**
     * Shows an appropriate dialog for any exception thrown by a service.
     * Database errors are also logged to stderr for debugging.
     */
    public static void showException(Component parent, Exception e) {
        if (e instanceof ValidationException) {
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Invalid input", JOptionPane.WARNING_MESSAGE);
        } else if (e instanceof AuthenticationException) {
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Login failed", JOptionPane.ERROR_MESSAGE);
        } else if (e instanceof AuthorizationException) {
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Access denied", JOptionPane.ERROR_MESSAGE);
        } else if (e instanceof DatabaseException) {
            System.err.println("Database error: " + e.getMessage());
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
        } else {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Unexpected error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}