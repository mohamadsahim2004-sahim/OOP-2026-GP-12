package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

/** Non-blocking transient feedback anchored to the enrollment page. */
public final class ToastManager {
    private final JPanel host;
    private Timer dismissTimer;
    private JPanel currentToast;

    public ToastManager(JPanel host) {
        this.host = host;
    }

    public void show(String message, boolean success) {
        if (dismissTimer != null) dismissTimer.stop();
        if (currentToast != null) host.remove(currentToast);
        RoundedPanel toast = new RoundedPanel(Theme.ENROLLMENT_CARD_RADIUS,
                success ? Theme.ENROLLMENT_SUCCESS_BG : Theme.ENROLLMENT_DANGER_BG);
        toast.setBorderColor(success ? Theme.ENROLLMENT_SUCCESS_BG : Theme.ENROLLMENT_DANGER_BG);
        toast.setLayout(new BorderLayout());
        toast.setBorder(Theme.createEmptyPadding(12, 16, 12, 16));
        JLabel label = new JLabel(message);
        label.setFont(Theme.ENROLLMENT_BODY_BOLD);
        label.setForeground(success ? Theme.ENROLLMENT_SUCCESS : Theme.ENROLLMENT_DANGER);
        toast.add(label, BorderLayout.CENTER);
        currentToast = toast;

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.anchor = GridBagConstraints.SOUTHEAST;
        c.insets = new Insets(Theme.ENROLLMENT_SECTION_GAP, 0, 0, 0);
        host.add(toast, c);
        host.revalidate();
        host.repaint();
        dismissTimer = new Timer(3000, e -> {
            host.remove(toast);
            if (currentToast == toast) currentToast = null;
            host.revalidate();
            host.repaint();
        });
        dismissTimer.setRepeats(false);
        dismissTimer.start();
    }
}
