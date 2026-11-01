package com.facultyams.ui.theme;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

public class IconHelper {

    public static Icon getIcon(String name, int size, Color color) {
        return new VectorIcon(name, size, color);
    }

    private static class VectorIcon implements Icon {
        private final String name;
        private final int size;
        private final Color color;

        public VectorIcon(String name, int size, Color color) {
            this.name = name;
            this.size = size;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setAntiAliasing(g2);
            g2.translate(x, y);
            g2.setColor(color);
            float s = size / 24f; // base grid 24x24
            g2.scale(s, s);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (name.toLowerCase()) {
                case "graduation-cap":
                    // Mortarboard cap & tassel
                    Path2D cap = new Path2D.Float();
                    cap.moveTo(12, 4);
                    cap.lineTo(22, 9);
                    cap.lineTo(12, 14);
                    cap.lineTo(2, 9);
                    cap.closePath();
                    g2.draw(cap);
                    // Cap skull base
                    Path2D base = new Path2D.Float();
                    base.moveTo(6, 11.5);
                    base.lineTo(6, 17);
                    base.curveTo(6, 20, 18, 20, 18, 17);
                    base.lineTo(18, 11.5);
                    g2.draw(base);
                    // Tassel
                    g2.drawLine(22, 9, 22, 15);
                    g2.fillOval(21, 15, 2, 3);
                    break;

                case "dashboard":
                    // 4 grid blocks
                    g2.drawRoundRect(3, 3, 7, 7, 3, 3);
                    g2.drawRoundRect(14, 3, 7, 7, 3, 3);
                    g2.drawRoundRect(3, 14, 7, 7, 3, 3);
                    g2.drawRoundRect(14, 14, 7, 7, 3, 3);
                    break;

                case "profile":
                    // Head & shoulders
                    g2.drawOval(8, 3, 8, 8);
                    Path2D body = new Path2D.Float();
                    body.moveTo(4, 21);
                    body.curveTo(4, 16, 8, 14, 12, 14);
                    body.curveTo(16, 14, 20, 16, 20, 21);
                    g2.draw(body);
                    break;

                case "courses":
                    // Open book
                    Path2D book = new Path2D.Float();
                    book.moveTo(12, 6);
                    book.curveTo(8, 4, 3, 4, 3, 6);
                    book.lineTo(3, 19);
                    book.curveTo(8, 18, 12, 19, 12, 21);
                    book.curveTo(12, 19, 16, 18, 21, 19);
                    book.lineTo(21, 6);
                    book.curveTo(16, 4, 12, 4, 12, 6);
                    g2.draw(book);
                    g2.drawLine(12, 6, 12, 21);
                    break;

                case "results":
                    // Bar chart analytics
                    g2.drawLine(4, 20, 20, 20);
                    g2.drawRoundRect(5, 12, 3, 8, 2, 2);
                    g2.drawRoundRect(10, 6, 3, 14, 2, 2);
                    g2.drawRoundRect(15, 9, 3, 11, 2, 2);
                    break;

                case "attendance":
                    // Calendar with checkmark
                    g2.drawRoundRect(3, 4, 18, 17, 4, 4);
                    g2.drawLine(3, 9, 21, 9);
                    g2.drawLine(7, 2, 7, 5);
                    g2.drawLine(17, 2, 17, 5);
                    // Checkmark
                    Path2D check = new Path2D.Float();
                    check.moveTo(8, 15);
                    check.lineTo(11, 18);
                    check.lineTo(16, 12);
                    g2.draw(check);
                    break;

                case "exams":
                    // Clipboard with check
                    g2.drawRoundRect(4, 4, 16, 18, 3, 3);
                    g2.drawRoundRect(8, 2, 8, 4, 2, 2);
                    g2.drawLine(7, 10, 17, 10);
                    g2.drawLine(7, 14, 17, 14);
                    g2.drawLine(7, 18, 13, 18);
                    break;

                case "timetable":
                    // Clock
                    g2.drawOval(3, 3, 18, 18);
                    g2.drawLine(12, 7, 12, 12);
                    g2.drawLine(12, 12, 16, 14);
                    break;

                case "notice":
                case "loudspeaker":
                    // Megaphone / Speaker
                    Path2D spk = new Path2D.Float();
                    spk.moveTo(3, 9);
                    spk.lineTo(7, 9);
                    spk.lineTo(14, 4);
                    spk.lineTo(14, 20);
                    spk.lineTo(7, 15);
                    spk.lineTo(3, 15);
                    spk.closePath();
                    g2.draw(spk);
                    // Sound waves
                    g2.drawArc(15, 8, 6, 8, -60, 120);
                    g2.drawArc(18, 5, 8, 14, -60, 120);
                    break;

                case "calendar":
                    // Regular calendar
                    g2.drawRoundRect(3, 4, 18, 17, 3, 3);
                    g2.drawLine(3, 9, 21, 9);
                    g2.drawLine(7, 2, 7, 5);
                    g2.drawLine(17, 2, 17, 5);
                    g2.fillOval(7, 12, 2, 2);
                    g2.fillOval(12, 12, 2, 2);
                    g2.fillOval(17, 12, 2, 2);
                    g2.fillOval(7, 16, 2, 2);
                    g2.fillOval(12, 16, 2, 2);
                    break;

                case "downloads":
                    // Tray with down arrow
                    g2.drawLine(12, 4, 12, 15);
                    g2.drawLine(8, 11, 12, 15);
                    g2.drawLine(16, 11, 12, 15);
                    Path2D tray = new Path2D.Float();
                    tray.moveTo(4, 14);
                    tray.lineTo(4, 20);
                    tray.lineTo(20, 20);
                    tray.lineTo(20, 14);
                    g2.draw(tray);
                    break;

                case "feedback":
                    // Speech bubble with dots
                    Path2D bubble = new Path2D.Float();
                    bubble.moveTo(4, 5);
                    bubble.curveTo(4, 4, 5, 3, 6, 3);
                    bubble.lineTo(18, 3);
                    bubble.curveTo(19, 3, 20, 4, 20, 5);
                    bubble.lineTo(20, 15);
                    bubble.curveTo(20, 16, 19, 17, 18, 17);
                    bubble.lineTo(9, 17);
                    bubble.lineTo(5, 21);
                    bubble.lineTo(5, 17);
                    bubble.curveTo(4, 17, 4, 16, 4, 15);
                    bubble.closePath();
                    g2.draw(bubble);
                    g2.fillOval(8, 9, 2, 2);
                    g2.fillOval(12, 9, 2, 2);
                    g2.fillOval(16, 9, 2, 2);
                    break;

                case "logout":
                    // Exit icon
                    g2.drawLine(13, 12, 3, 12);
                    g2.drawLine(7, 8, 3, 12);
                    g2.drawLine(7, 16, 3, 12);
                    Path2D door = new Path2D.Float();
                    door.moveTo(11, 4);
                    door.lineTo(20, 4);
                    door.lineTo(20, 20);
                    door.lineTo(11, 20);
                    g2.draw(door);
                    break;

                case "rank":
                case "trophy":
                    // Trophy cup
                    Path2D cup = new Path2D.Float();
                    cup.moveTo(6, 4);
                    cup.lineTo(18, 4);
                    cup.curveTo(18, 12, 14, 13, 12, 13);
                    cup.curveTo(10, 13, 6, 12, 6, 4);
                    g2.draw(cup);
                    // Handles
                    g2.drawArc(3, 5, 5, 6, 90, 180);
                    g2.drawArc(16, 5, 5, 6, 270, 180);
                    // Base
                    g2.drawLine(12, 13, 12, 17);
                    g2.drawLine(8, 17, 16, 17);
                    g2.drawRoundRect(6, 17, 12, 3, 1, 1);
                    break;

                case "cgpa":
                case "document":
                    // Document with folded corner
                    Path2D doc = new Path2D.Float();
                    doc.moveTo(5, 3);
                    doc.lineTo(15, 3);
                    doc.lineTo(19, 7);
                    doc.lineTo(19, 21);
                    doc.lineTo(5, 21);
                    doc.closePath();
                    g2.draw(doc);
                    g2.drawLine(15, 3, 15, 7);
                    g2.drawLine(15, 7, 19, 7);
                    g2.drawLine(8, 11, 15, 11);
                    g2.drawLine(8, 15, 16, 15);
                    break;

                case "search":
                    // Magnifying glass
                    g2.drawOval(4, 4, 11, 11);
                    g2.drawLine(12, 12, 19, 19);
                    break;

                case "bell":
                    // Notification bell
                    Path2D bell = new Path2D.Float();
                    bell.moveTo(12, 3);
                    bell.lineTo(12, 4);
                    bell.curveTo(9, 4, 6, 7, 6, 12);
                    bell.lineTo(4, 17);
                    bell.lineTo(20, 17);
                    bell.lineTo(18, 12);
                    bell.curveTo(18, 7, 15, 4, 12, 4);
                    g2.draw(bell);
                    g2.drawArc(10, 18, 4, 3, 180, 180);
                    break;

                case "medical":
                    // Cross badge
                    g2.drawRoundRect(3, 3, 18, 18, 4, 4);
                    g2.drawLine(12, 7, 12, 17);
                    g2.drawLine(7, 12, 17, 12);
                    break;

                default:
                    // Generic circle
                    g2.drawOval(4, 4, 16, 16);
                    break;
            }

            g2.dispose();
        }
    }
}
