package com.facultyams.ui.admin;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Small line icons drawn with Java2D on a 24x24 grid, so the sidebar, buttons and tables need no image files.
 * {@code AdminIcons.of(Kind.EDIT, 16, color)} returns a ready-to-use {@link Icon}.
 */
public final class AdminIcons {

    /** Every icon the Admin screens use. */
    public enum Kind {
        DASHBOARD, USERS, BUILDING, BOOK, MEGAPHONE, CALENDAR, GEAR, CHART, PROFILE, LOGOUT, DATABASE, SHIELD,
        EDIT, TRASH, KEY, POWER, SEARCH, LOCK, EYE, EYE_OFF, CAP, PLUS, CHEVRON_LEFT, CHEVRON_RIGHT,
        CHEVRON_DOWN, CHECK, INBOX, GRID, LIST
    }

    private AdminIcons() {
        // utility class
    }

    public static Icon of(Kind kind, int size, Color color) {
        return new Glyph(kind, size, color);
    }

    private static final class Glyph implements Icon {
        private final Kind kind;
        private final int size;
        private final Color color;

        Glyph(Kind kind, int size, Color color) {
            this.kind = kind;
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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.scale(size / 24.0, size / 24.0);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(color);
            draw(g2);
            g2.dispose();
        }

        private static Path2D path(double[]... points) {
            Path2D p = new Path2D.Double();
            for (int i = 0; i < points.length; i++) {
                if (i == 0) {
                    p.moveTo(points[i][0], points[i][1]);
                } else {
                    p.lineTo(points[i][0], points[i][1]);
                }
            }
            return p;
        }

        private static double[] pt(double x, double y) {
            return new double[] {x, y};
        }

        private void draw(Graphics2D g) {
            switch (kind) {
                case DASHBOARD -> {
                    g.draw(new RoundRectangle2D.Double(3, 3, 7.5, 7.5, 2, 2));
                    g.draw(new RoundRectangle2D.Double(13.5, 3, 7.5, 7.5, 2, 2));
                    g.draw(new RoundRectangle2D.Double(3, 13.5, 7.5, 7.5, 2, 2));
                    g.draw(new RoundRectangle2D.Double(13.5, 13.5, 7.5, 7.5, 2, 2));
                }
                case GRID -> {
                    g.draw(new RoundRectangle2D.Double(3, 4, 18, 16, 2, 2));
                    g.draw(new Line2D.Double(3, 10, 21, 10));
                    g.draw(new Line2D.Double(9, 4, 9, 20));
                    g.draw(new Line2D.Double(15, 10, 15, 20));
                }
                case LIST -> {
                    for (int i = 0; i < 3; i++) {
                        g.draw(new Line2D.Double(8, 6 + i * 6, 20, 6 + i * 6));
                        g.draw(new Line2D.Double(4, 6 + i * 6, 4.5, 6 + i * 6));
                    }
                }
                case USERS -> {
                    g.draw(new Ellipse2D.Double(5.5, 4.5, 7, 7));
                    g.draw(new Arc2D.Double(2.5, 14, 13, 12, 0, 180, Arc2D.OPEN));
                    g.draw(new Arc2D.Double(14, 5, 5.5, 5.5, 90, -250, Arc2D.OPEN));
                    g.draw(new Arc2D.Double(15.5, 14.5, 6, 9, 0, 90, Arc2D.OPEN));
                }
                case PROFILE -> {
                    g.draw(new Ellipse2D.Double(7.5, 3.5, 9, 9));
                    g.draw(new Arc2D.Double(4, 14.5, 16, 14, 0, 180, Arc2D.OPEN));
                }
                case BUILDING -> {
                    Path2D roof = path(pt(3, 9), pt(12, 4), pt(21, 9));
                    roof.closePath();
                    g.draw(roof);
                    g.draw(new Line2D.Double(4, 20.5, 20, 20.5));
                    for (int x : new int[] {7, 12, 17}) {
                        g.draw(new Line2D.Double(x, 11.5, x, 18));
                    }
                }
                case BOOK -> {
                    Path2D p = new Path2D.Double();
                    p.moveTo(12, 6.5);
                    p.curveTo(9.5, 4.5, 6, 4.5, 3, 5.5);
                    p.lineTo(3, 18.5);
                    p.curveTo(6, 17.5, 9.5, 17.5, 12, 19.5);
                    p.curveTo(14.5, 17.5, 18, 17.5, 21, 18.5);
                    p.lineTo(21, 5.5);
                    p.curveTo(18, 4.5, 14.5, 4.5, 12, 6.5);
                    g.draw(p);
                    g.draw(new Line2D.Double(12, 6.5, 12, 19.5));
                }
                case MEGAPHONE -> {
                    Path2D p = path(pt(3, 9.5), pt(7.5, 9.5), pt(17, 4.5), pt(17, 19.5), pt(7.5, 14.5), pt(3, 14.5));
                    p.closePath();
                    g.draw(p);
                    g.draw(new Line2D.Double(7.5, 14.5, 9, 20));
                    g.draw(new Arc2D.Double(15, 8, 7, 8, -90, 180, Arc2D.OPEN));
                }
                case CALENDAR -> {
                    g.draw(new RoundRectangle2D.Double(3, 5, 18, 16, 3, 3));
                    g.draw(new Line2D.Double(3, 10.5, 21, 10.5));
                    g.draw(new Line2D.Double(8, 3, 8, 7));
                    g.draw(new Line2D.Double(16, 3, 16, 7));
                }
                case GEAR -> {
                    g.draw(new Ellipse2D.Double(5.5, 5.5, 13, 13));
                    g.draw(new Ellipse2D.Double(9.5, 9.5, 5, 5));
                    for (int i = 0; i < 8; i++) {
                        double a = Math.toRadians(i * 45);
                        g.draw(new Line2D.Double(12 + 6.5 * Math.cos(a), 12 + 6.5 * Math.sin(a),
                                12 + 9 * Math.cos(a), 12 + 9 * Math.sin(a)));
                    }
                }
                case CHART -> {
                    g.draw(new Line2D.Double(5, 20, 5, 12));
                    g.draw(new Line2D.Double(12, 20, 12, 4));
                    g.draw(new Line2D.Double(19, 20, 19, 9));
                }
                case LOGOUT -> {
                    Path2D p = new Path2D.Double();
                    p.moveTo(9.5, 4);
                    p.lineTo(5.5, 4);
                    p.quadTo(3.5, 4, 3.5, 6);
                    p.lineTo(3.5, 18);
                    p.quadTo(3.5, 20, 5.5, 20);
                    p.lineTo(9.5, 20);
                    g.draw(p);
                    g.draw(new Line2D.Double(10, 12, 21, 12));
                    g.draw(path(pt(17, 8), pt(21, 12), pt(17, 16)));
                }
                case DATABASE -> {
                    g.draw(new Ellipse2D.Double(4, 3, 16, 6));
                    g.draw(new Arc2D.Double(4, 9, 16, 6, 180, 180, Arc2D.OPEN));
                    g.draw(new Arc2D.Double(4, 15, 16, 6, 180, 180, Arc2D.OPEN));
                    g.draw(new Line2D.Double(4, 6, 4, 18));
                    g.draw(new Line2D.Double(20, 6, 20, 18));
                }
                case SHIELD -> {
                    Path2D p = new Path2D.Double();
                    p.moveTo(12, 3);
                    p.lineTo(20, 6);
                    p.lineTo(20, 12);
                    p.curveTo(20, 17, 16, 20, 12, 21.5);
                    p.curveTo(8, 20, 4, 17, 4, 12);
                    p.lineTo(4, 6);
                    p.closePath();
                    g.draw(p);
                }
                case EDIT -> {
                    Path2D p = path(pt(4, 20), pt(4.8, 15.8), pt(16, 4.6), pt(19.4, 8), pt(8.2, 19.2));
                    p.closePath();
                    g.draw(p);
                    g.draw(new Line2D.Double(13.5, 7, 17, 10.5));
                }
                case TRASH -> {
                    g.draw(new Line2D.Double(4, 7, 20, 7));
                    g.draw(path(pt(9, 7), pt(9, 4), pt(15, 4), pt(15, 7)));
                    g.draw(path(pt(6, 7), pt(7, 20), pt(17, 20), pt(18, 7)));
                    g.draw(new Line2D.Double(10, 11, 10, 16));
                    g.draw(new Line2D.Double(14, 11, 14, 16));
                }
                case KEY -> {
                    g.draw(new Ellipse2D.Double(3.5, 10.5, 8, 8));
                    g.draw(new Line2D.Double(10.5, 11.5, 20, 3));
                    g.draw(new Line2D.Double(16, 7.5, 18.5, 10));
                }
                case POWER -> {
                    g.draw(new Arc2D.Double(5, 6, 14, 14, 120, 300, Arc2D.OPEN));
                    g.draw(new Line2D.Double(12, 3, 12, 12));
                }
                case SEARCH -> {
                    g.draw(new Ellipse2D.Double(4, 4, 12, 12));
                    g.draw(new Line2D.Double(14, 14, 20, 20));
                }
                case LOCK -> {
                    g.draw(new RoundRectangle2D.Double(5, 11, 14, 10, 2.5, 2.5));
                    g.draw(new Arc2D.Double(8, 3, 8, 10, 0, 180, Arc2D.OPEN));
                    g.draw(new Line2D.Double(8, 8, 8, 11));
                    g.draw(new Line2D.Double(16, 8, 16, 11));
                }
                case EYE, EYE_OFF -> {
                    Path2D p = new Path2D.Double();
                    p.moveTo(2, 12);
                    p.quadTo(12, 1.5, 22, 12);
                    p.quadTo(12, 22.5, 2, 12);
                    g.draw(p);
                    g.draw(new Ellipse2D.Double(9, 9, 6, 6));
                    if (kind == Kind.EYE_OFF) {
                        g.draw(new Line2D.Double(4, 20, 20, 4));
                    }
                }
                case CAP -> {
                    Path2D top = path(pt(12, 4), pt(23, 9), pt(12, 14), pt(1, 9));
                    top.closePath();
                    g.fill(top);
                    Path2D cap = new Path2D.Double();
                    cap.moveTo(5.5, 12);
                    cap.lineTo(5.5, 16.5);
                    cap.curveTo(5.5, 19.5, 18.5, 19.5, 18.5, 16.5);
                    cap.lineTo(18.5, 12);
                    g.draw(cap);
                    g.draw(new Line2D.Double(22, 9.5, 22, 15));
                }
                case PLUS -> {
                    g.draw(new Line2D.Double(12, 5, 12, 19));
                    g.draw(new Line2D.Double(5, 12, 19, 12));
                }
                case CHEVRON_LEFT -> g.draw(path(pt(14.5, 6), pt(8.5, 12), pt(14.5, 18)));
                case CHEVRON_RIGHT -> g.draw(path(pt(9.5, 6), pt(15.5, 12), pt(9.5, 18)));
                case CHEVRON_DOWN -> g.draw(path(pt(6, 9.5), pt(12, 15.5), pt(18, 9.5)));
                case CHECK -> g.draw(path(pt(5, 12.5), pt(10, 17.5), pt(19, 7)));
                case INBOX -> {
                    g.draw(path(pt(3, 13), pt(6, 5), pt(18, 5), pt(21, 13), pt(21, 19), pt(3, 19), pt(3, 13)));
                    Path2D tray = new Path2D.Double();
                    tray.moveTo(3, 13);
                    tray.lineTo(8, 13);
                    tray.curveTo(8, 16, 16, 16, 16, 13);
                    tray.lineTo(21, 13);
                    g.draw(tray);
                }
                default -> {
                    // every Kind is handled above
                }
            }
        }
    }
}