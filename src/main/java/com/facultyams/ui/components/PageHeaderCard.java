package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

/** Enrollment page heading and live credit meter. */
public final class PageHeaderCard extends RoundedPanel {
    private final CreditMeter creditMeter = new CreditMeter();

    public PageHeaderCard() {
        super(Theme.ENROLLMENT_CARD_RADIUS);
        setBorderColor(Theme.ENROLLMENT_BORDER);
        setLayout(new GridBagLayout());
        setBorder(Theme.createEmptyPadding(Theme.ENROLLMENT_CARD_PADDING,
                Theme.ENROLLMENT_CARD_PADDING, Theme.ENROLLMENT_CARD_PADDING,
                Theme.ENROLLMENT_CARD_PADDING));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Course Enrollment - Semester 5");
        title.setFont(Theme.ENROLLMENT_PAGE_TITLE);
        title.setForeground(Theme.ENROLLMENT_TEXT);
        JLabel subtitle = new JLabel("Manage enrolled modules and electives. Maximum credit limit is 20 credits per semester.");
        subtitle.setFont(Theme.ENROLLMENT_BODY);
        subtitle.setForeground(Theme.ENROLLMENT_MUTED);
        text.add(title);
        text.add(Box.createVerticalStrut(8));
        text.add(subtitle);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        add(text, c);
        c.gridx = 1;
        c.weightx = 0;
        c.insets = new Insets(0, Theme.ENROLLMENT_SECTION_GAP, 0, 0);
        c.anchor = GridBagConstraints.EAST;
        add(creditMeter, c);
    }

    public void updateCredits(int credits) {
        creditMeter.updateCredits(credits);
    }
}
