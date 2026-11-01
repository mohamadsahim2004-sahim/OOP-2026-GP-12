package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import javax.swing.*;

public class FooterPanel extends JPanel {

    public FooterPanel() {
        setPreferredSize(new Dimension(0, 38));
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.CARD_BORDER),
                Theme.createEmptyPadding(6, 24, 6, 24)
        ));

        JLabel copyLbl = new JLabel("\u00A9 2024 Student Result Management System. All rights reserved.");
        copyLbl.setFont(Theme.FONT_SMALL);
        copyLbl.setForeground(Theme.TEXT_MUTED);

        JLabel heartLbl = new JLabel("Made with \u2764 for Students");
        heartLbl.setFont(Theme.FONT_SMALL);
        heartLbl.setForeground(Theme.TEXT_MUTED);

        add(copyLbl, BorderLayout.WEST);
        add(heartLbl, BorderLayout.EAST);
    }
}
