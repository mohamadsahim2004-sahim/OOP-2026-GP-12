package com.srms.ui.components;

import com.srms.Theme;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * High-definition Java2D vector icon for course activities.
 * No external images or font glyph dependencies.
 */
public class ActivityIcon extends JComponent {
    public enum Type {
        ANNOUNCEMENT, RESOURCE, ASSIGNMENT
    }

    private final Type type;
    private final String fileType;

    public ActivityIcon(Type type) {
        this(type, null);
    }

    public ActivityIcon(Type type, String fileType) {
        this.type = type;
        this.fileType = fileType;
        setPreferredSize(new Dimension(28, 28));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setAntiAliasing(g2);

        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h) - 4;
        int x = (w - size) / 2;
        int y = (h - size) / 2;

        switch (type) {
            case ANNOUNCEMENT -> paintAnnouncement(g2, x, y, size);
            case RESOURCE -> paintResource(g2, x, y, size);
            case ASSIGNMENT -> paintAssignment(g2, x, y, size);
        }

        g2.dispose();
    }

    private void paintAnnouncement(Graphics2D g2, int x, int y, int size) {
        // Purple chat bubble
        g2.setColor(Theme.ANNOUNCEMENT_PURPLE);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int bubbleW = size - 2;
        int bubbleH = size - 6;

        GeneralPath path = new GeneralPath();
        path.append(new RoundRectangle2D.Float(x + 1, y + 1, bubbleW, bubbleH, 6, 6), false);
        // Little speech tail
        path.moveTo(x + 5, y + 1 + bubbleH);
        path.lineTo(x + 3, y + size);
        path.lineTo(x + 10, y + 1 + bubbleH);
        g2.draw(path);

        // Lines inside bubble
        g2.drawLine(x + 5, y + 6, x + bubbleW - 3, y + 6);
        g2.drawLine(x + 5, y + 10, x + bubbleW - 7, y + 10);
    }

    private void paintResource(Graphics2D g2, int x, int y, int size) {
        // Teal document with folded corner
        g2.setColor(Theme.RESOURCE_TEAL);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int docW = size - 6;
        int docH = size;
        int fold = 6;
        int startX = x + 3;

        GeneralPath path = new GeneralPath();
        path.moveTo(startX, y);
        path.lineTo(startX + docW - fold, y);
        path.lineTo(startX + docW, y + fold);
        path.lineTo(startX + docW, y + docH);
        path.lineTo(startX, y + docH);
        path.closePath();
        g2.draw(path);

        // Fold notch
        g2.drawLine(startX + docW - fold, y, startX + docW - fold, y + fold);
        g2.drawLine(startX + docW - fold, y + fold, startX + docW, y + fold);

        // Horizontal lines or PDF text
        if (fileType != null && fileType.equalsIgnoreCase("PDF")) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 7));
            g2.drawString("PDF", startX + 2, y + docH - 5);
        } else {
            g2.drawLine(startX + 3, y + 10, startX + docW - 3, y + 10);
            g2.drawLine(startX + 3, y + 14, startX + docW - 5, y + 14);
        }
    }

    private void paintAssignment(Graphics2D g2, int x, int y, int size) {
        // Pink upload tray + up arrow
        g2.setColor(Theme.ASSIGNMENT_PINK);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Tray bracket
        int trayY = y + size - 5;
        g2.drawLine(x + 2, trayY - 4, x + 2, trayY);
        g2.drawLine(x + 2, trayY, x + size - 2, trayY);
        g2.drawLine(x + size - 2, trayY, x + size - 2, trayY - 4);

        // Up arrow
        int cx = x + size / 2;
        int topY = y + 2;
        g2.drawLine(cx, trayY - 4, cx, topY);
        g2.drawLine(cx - 4, topY + 4, cx, topY);
        g2.drawLine(cx + 4, topY + 4, cx, topY);
    }
}
