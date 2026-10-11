package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.JPanel;

public class BannerIllustration extends JPanel {

    public BannerIllustration() {
        setOpaque(false);
        setPreferredSize(new Dimension(280, 140));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setAntiAliasing(g2);

        int w = getWidth();
        int h = getHeight();

        // Decorative soft cloud in background
        g2.setColor(new Color(225, 238, 255, 160));
        g2.fillOval(w - 240, 20, 60, 40);
        g2.fillOval(w - 200, 10, 70, 50);
        g2.fillOval(w - 150, 25, 50, 35);

        // Birds in sky
        g2.setColor(new Color(148, 163, 184));
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawBird(g2, w - 50, 25, 12);
        drawBird(g2, w - 30, 35, 9);

        // Plant pot (white vase with green leaves)
        int potX = w - 210;
        int potY = h - 60;

        // Stem & Leaves
        g2.setColor(new Color(34, 197, 94)); // vibrant green
        // Center stem
        g2.setStroke(new BasicStroke(2.2f));
        g2.drawLine(potX + 18, potY, potX + 18, potY - 55);

        // Green leaves (oval petals)
        g2.fillOval(potX + 15, potY - 65, 8, 16);
        g2.fillOval(potX + 6, potY - 50, 14, 8);
        g2.fillOval(potX + 20, potY - 45, 14, 8);
        g2.fillOval(potX + 4, potY - 32, 16, 9);
        g2.fillOval(potX + 18, potY - 26, 16, 9);

        // White pot
        g2.setColor(Color.WHITE);
        Polygon pot = new Polygon();
        pot.addPoint(potX + 6, potY);
        pot.addPoint(potX + 30, potY);
        pot.addPoint(potX + 26, potY + 28);
        pot.addPoint(potX + 10, potY + 28);
        g2.fillPolygon(pot);
        g2.setColor(new Color(203, 213, 225));
        g2.drawPolygon(pot);

        // Stacked Books
        int bookBaseX = w - 160;
        int bookBaseY = h - 35;

        // Book 1 (Bottom, Blue)
        drawBook(g2, bookBaseX, bookBaseY, 130, 22, new Color(37, 99, 235), new Color(248, 250, 252));

        // Book 2 (Middle, Amber/Yellow)
        drawBook(g2, bookBaseX + 12, bookBaseY - 20, 115, 20, new Color(245, 158, 11), new Color(248, 250, 252));

        // Book 3 (Top, Indigo/Blue)
        drawBook(g2, bookBaseX + 20, bookBaseY - 38, 105, 19, new Color(59, 130, 246), new Color(248, 250, 252));

        // Bookmark ribbon hanging from top book
        g2.setColor(new Color(239, 68, 68)); // red ribbon
        Path2D ribbon = new Path2D.Float();
        ribbon.moveTo(bookBaseX + 50, bookBaseY - 37);
        ribbon.lineTo(bookBaseX + 50, bookBaseY - 14);
        ribbon.lineTo(bookBaseX + 54, bookBaseY - 18);
        ribbon.lineTo(bookBaseX + 58, bookBaseY - 14);
        ribbon.lineTo(bookBaseX + 58, bookBaseY - 37);
        ribbon.closePath();
        g2.fill(ribbon);

        g2.dispose();
    }

    private void drawBird(Graphics2D g2, int x, int y, int size) {
        Path2D p = new Path2D.Float();
        p.moveTo(x - size, y + 2);
        p.curveTo(x - size / 2, y - size / 2, x - size / 4, y - size / 2, x, y);
        p.curveTo(x + size / 4, y - size / 2, x + size / 2, y - size / 2, x + size, y + 2);
        g2.draw(p);
    }

    private void drawBook(Graphics2D g2, int x, int y, int width, int height, Color coverColor, Color pagesColor) {
        // Spine / Cover
        g2.setColor(coverColor);
        g2.fillRoundRect(x, y, width, height, 6, 6);

        // White pages inset
        g2.setColor(pagesColor);
        g2.fillRoundRect(x + 12, y + 3, width - 15, height - 6, 3, 3);

        // Subtle page lines
        g2.setColor(new Color(226, 232, 240));
        g2.drawLine(x + 14, y + height / 2, x + width - 6, y + height / 2);

        // Cover border
        g2.setColor(coverColor.darker());
        g2.drawRoundRect(x, y, width, height, 6, 6);
    }
}
