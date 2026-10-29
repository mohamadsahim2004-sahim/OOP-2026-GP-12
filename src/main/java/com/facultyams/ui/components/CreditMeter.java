package com.facultyams.ui.components;

import com.facultyams.service.EnrollmentService;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

/** Live enrollment credit summary. */
public final class CreditMeter extends JPanel {
    private final JLabel total = new JLabel();
    private final JLabel remaining = new JLabel();
    private final EnrollmentProgressBar progress = new EnrollmentProgressBar(EnrollmentService.MAX_CREDITS);

    public CreditMeter() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        total.setFont(Theme.ENROLLMENT_BODY_BOLD);
        total.setForeground(Theme.PRIMARY);
        remaining.setFont(Theme.ENROLLMENT_SMALL);
        remaining.setForeground(Theme.ENROLLMENT_MUTED);
        total.setAlignmentX(Component.LEFT_ALIGNMENT);
        remaining.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(total);
        add(Box.createVerticalStrut(4));
        add(remaining);
        add(Box.createVerticalStrut(8));
        add(progress);
        setPreferredSize(new Dimension(260, 56));
    }

    public void updateCredits(int credits) {
        int bounded = Math.max(0, Math.min(EnrollmentService.MAX_CREDITS, credits));
        total.setText(bounded + " / " + EnrollmentService.MAX_CREDITS + " credits");
        int left = Math.max(0, EnrollmentService.MAX_CREDITS - credits);
        remaining.setText(left + " credits remaining");
        progress.animateTo(bounded, EnrollmentService.MAX_CREDITS);
    }
}
