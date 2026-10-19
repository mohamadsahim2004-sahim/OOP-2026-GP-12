package com.srms.ui.components;

import com.srms.Theme;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Exquisite procedural cover banners rendering authentic patterns matching
 * Moodle / university portal course themes.
 */
public final class CoverPainter {
    private CoverPainter() {}

    public static void paintBanner(Graphics2D g2, int width, int height, String code, String coverPath, int topRadius) {
        Theme.setAntiAliasing(g2);

        Shape clip = new RoundRectangle2D.Float(0, 0, width, height, topRadius * 2, topRadius * 2);
        Shape oldClip = g2.getClip();
        g2.setClip(clip);

        boolean drawnImage = false;
        if (coverPath != null && !coverPath.isBlank()) {
            File imgFile = new File(coverPath);
            if (imgFile.exists()) {
                try {
                    BufferedImage img = ImageIO.read(imgFile);
                    if (img != null) {
                        g2.drawImage(img, 0, 0, width, height, null);
                        drawnImage = true;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (!drawnImage) {
            String safeCode = code != null ? code.toUpperCase() : "";
            if (safeCode.contains("ENG")) {
                paintPinkLowPoly(g2, width, height);
            } else if (safeCode.contains("2113") || safeCode.contains("DSA") || safeCode.contains("STRUCTURE")) {
                paintDataStructuresTechBanner(g2, width, height);
            } else if (safeCode.contains("2122") || safeCode.contains("OOP") || safeCode.contains("OBJECT")) {
                paintBlueCirclesBanner(g2, width, height);
            } else if (safeCode.contains("2132") || safeCode.contains("PRACTICUM")) {
                paintGrayPolyBanner(g2, width, height);
            } else if (safeCode.contains("2142") || safeCode.contains("BUSINESS")) {
                paintTealEmeraldPoly(g2, width, height);
            } else if (safeCode.contains("2152") || safeCode.contains("ANALYSIS") || safeCode.contains("DESIGN")) {
                paintSlateHexBanner(g2, width, height);
            } else {
                paintGenericPoly(g2, width, height, safeCode);
            }
        }

        g2.setClip(oldClip);
    }

    // 1. ENG2112: Beautiful Pink / Rose Low-Poly
    private static void paintPinkLowPoly(Graphics2D g2, int width, int height) {
        Color c1 = new Color(244, 114, 182); // Rose 400
        Color c2 = new Color(219, 39, 119);  // Pink 600
        GradientPaint bg = new GradientPaint(0, 0, c1, width, height, c2);
        g2.setPaint(bg);
        g2.fillRect(0, 0, width, height);

        Color[] pinkPalette = {
                new Color(251, 207, 232, 70),
                new Color(244, 114, 182, 80),
                new Color(236, 72, 153, 90),
                new Color(190, 24, 93, 75)
        };
        renderMeshTriangles(g2, width, height, pinkPalette, 42);
    }

    // 2. ICT2113: Dark Tech Graph Banner with Node Network and "Data Structures"
    private static void paintDataStructuresTechBanner(Graphics2D g2, int width, int height) {
        g2.setColor(new Color(11, 15, 25)); // Deep midnight
        g2.fillRect(0, 0, width, height);

        // Subtle gradient glow in center
        RadialGradientPaint glow = new RadialGradientPaint(
                new Point(width / 2, height / 2),
                width / 2f,
                new float[]{ 0.0f, 1.0f },
                new Color[]{ new Color(30, 41, 59), new Color(11, 15, 25) }
        );
        g2.setPaint(glow);
        g2.fillRect(0, 0, width, height);

        // Tech network lines
        g2.setColor(new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(1.2f));

        int cx = width / 2;
        int cy = height / 2 + 15;

        // Tree / graph connections
        g2.drawLine(cx, cy, cx - 70, cy + 30);
        g2.drawLine(cx, cy, cx + 70, cy + 30);
        g2.drawLine(cx - 70, cy + 30, cx - 110, cy + 50);
        g2.drawLine(cx + 70, cy + 30, cx + 110, cy + 50);

        // Nodes
        drawNode(g2, cx, cy, new Color(59, 130, 246), "1");
        drawNode(g2, cx - 70, cy + 30, new Color(239, 68, 68), "2");
        drawNode(g2, cx + 70, cy + 30, new Color(16, 185, 129), "3");
        drawNode(g2, cx - 110, cy + 50, new Color(245, 158, 11), "4");
        drawNode(g2, cx + 110, cy + 50, new Color(168, 85, 247), "5");

        // Bold Title text in center
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        FontMetrics fm = g2.getFontMetrics();
        String text = "Data Structures";
        int tx = (width - fm.stringWidth(text)) / 2;
        g2.drawString(text, tx, height / 2 - 10);
    }

    private static void drawNode(Graphics2D g2, int x, int y, Color c, String lbl) {
        g2.setColor(c);
        g2.fillOval(x - 9, y - 9, 18, 18);
        g2.setColor(Color.WHITE);
        g2.drawOval(x - 9, y - 9, 18, 18);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
        g2.drawString(lbl, x - 3, y + 4);
    }

    // 3. ICT2122: Blue Circles / Bokeh Graphic
    private static void paintBlueCirclesBanner(Graphics2D g2, int width, int height) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(37, 99, 235), width, height, new Color(29, 78, 216));
        g2.setPaint(bg);
        g2.fillRect(0, 0, width, height);

        // Overlapping translucent circles
        int[] rads = { 90, 110, 70, 130, 80, 100, 60 };
        int[] xs = { width / 5, width / 2, (width * 4) / 5, width / 3, (width * 2) / 3, width - 20, 10 };
        int[] ys = { height / 3, height / 2, height / 4, (height * 3) / 4, (height * 2) / 3, height / 2, height - 10 };

        for (int i = 0; i < rads.length; i++) {
            g2.setColor(new Color(255, 255, 255, 22 + (i * 4)));
            g2.fillOval(xs[i] - rads[i] / 2, ys[i] - rads[i] / 2, rads[i], rads[i]);
        }
    }

    // 4. ICT2132: Gray Geometric Polygon Banner
    private static void paintGrayPolyBanner(Graphics2D g2, int width, int height) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(148, 163, 184), width, height, new Color(100, 116, 139));
        g2.setPaint(bg);
        g2.fillRect(0, 0, width, height);

        Color[] grayPalette = {
                new Color(241, 245, 249, 70),
                new Color(203, 213, 225, 80),
                new Color(148, 163, 184, 90),
                new Color(71, 85, 105, 75)
        };
        renderMeshTriangles(g2, width, height, grayPalette, 88);
    }

    // 5. ICT2142: Vibrant Emerald / Teal Low-Poly
    private static void paintTealEmeraldPoly(Graphics2D g2, int width, int height) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(16, 185, 129), width, height, new Color(13, 148, 136));
        g2.setPaint(bg);
        g2.fillRect(0, 0, width, height);

        Color[] tealPalette = {
                new Color(167, 243, 208, 70),
                new Color(52, 211, 153, 80),
                new Color(20, 184, 166, 90),
                new Color(15, 118, 110, 75)
        };
        renderMeshTriangles(g2, width, height, tealPalette, 123);
    }

    // 6. ICT2152: Slate Geometric Pattern
    private static void paintSlateHexBanner(Graphics2D g2, int width, int height) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(203, 213, 225), width, height, new Color(148, 163, 184));
        g2.setPaint(bg);
        g2.fillRect(0, 0, width, height);

        Color[] slatePalette = {
                new Color(248, 250, 252, 90),
                new Color(226, 232, 240, 80),
                new Color(148, 163, 184, 70),
                new Color(100, 116, 139, 60)
        };
        renderMeshTriangles(g2, width, height, slatePalette, 256);
    }

    private static void paintGenericPoly(Graphics2D g2, int width, int height, String code) {
        int hash = Math.abs(code.hashCode());
        GradientPaint bg = new GradientPaint(0, 0, new Color(59, 130, 246), width, height, new Color(30, 64, 175));
        g2.setPaint(bg);
        g2.fillRect(0, 0, width, height);

        Color[] palette = {
                new Color(191, 219, 254, 70),
                new Color(147, 197, 253, 80),
                new Color(96, 165, 250, 90),
                new Color(37, 99, 235, 75)
        };
        renderMeshTriangles(g2, width, height, palette, hash);
    }

    private static void renderMeshTriangles(Graphics2D g2, int width, int height, Color[] palette, int seed) {
        int cols = 6;
        int rows = 3;
        float dx = (float) width / cols;
        float dy = (float) height / rows;

        Point[][] grid = new Point[cols + 1][rows + 1];
        Random rnd = new Random(seed);

        for (int i = 0; i <= cols; i++) {
            for (int j = 0; j <= rows; j++) {
                int px = (int) (i * dx);
                int py = (int) (j * dy);
                if (i > 0 && i < cols) px += (rnd.nextInt(19) - 9);
                if (j > 0 && j < rows) py += (rnd.nextInt(15) - 7);
                grid[i][j] = new Point(px, py);
            }
        }

        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                Point p1 = grid[i][j];
                Point p2 = grid[i + 1][j];
                Point p3 = grid[i][j + 1];
                Point p4 = grid[i + 1][j + 1];

                drawTriangle(g2, p1, p2, p3, palette[rnd.nextInt(palette.length)]);
                drawTriangle(g2, p2, p4, p3, palette[rnd.nextInt(palette.length)]);
            }
        }
    }

    private static void drawTriangle(Graphics2D g2, Point p1, Point p2, Point p3, Color c) {
        GeneralPath path = new GeneralPath();
        path.moveTo(p1.x, p1.y);
        path.lineTo(p2.x, p2.y);
        path.lineTo(p3.x, p3.y);
        path.closePath();

        g2.setColor(c);
        g2.fill(path);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.setStroke(new BasicStroke(0.8f));
        g2.draw(path);
    }
}
