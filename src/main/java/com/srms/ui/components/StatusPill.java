package com.srms.ui.components;

import com.srms.Theme;
import com.srms.model.SubmissionStatus;
import java.awt.*;
import javax.swing.*;

/**
 * Status pill rendering assignment submission states with appropriate color badges.
 */
public class StatusPill extends JLabel {
    private Color bg;
    private Color fg;

    public StatusPill(SubmissionStatus status) {
        super("", SwingConstants.CENTER);
        setFont(Theme.FONT_TINY);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        setStatus(status);
    }

    public void setStatus(SubmissionStatus status) {
        if (status == null) status = SubmissionStatus.NOT_SUBMITTED;
        switch (status) {
            case SUBMITTED -> {
                this.bg = new Color(220, 252, 231);
                this.fg = new Color(22, 163, 74);
                setText("Submitted");
            }
            case LATE -> {
                this.bg = new Color(254, 226, 226);
                this.fg = new Color(220, 38, 38);
                setText("Overdue");
            }
            case GRADED -> {
                this.bg = new Color(224, 231, 255);
                this.fg = new Color(37, 99, 235);
                setText("Graded");
            }
            case NOT_SUBMITTED -> {
                this.bg = new Color(243, 244, 246);
                this.fg = new Color(107, 114, 128);
                setText("Not submitted");
            }
        }
        setForeground(fg);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setAntiAliasing(g2);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width + 12, 90), 24);
    }
}
