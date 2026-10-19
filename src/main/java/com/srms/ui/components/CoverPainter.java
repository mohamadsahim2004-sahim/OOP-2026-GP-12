package com.srms.ui.components;

import com.srms.Theme;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Generates procedural geometric/gradient cover banners or renders local images
 * with rounded top corners.
 */
public final class CoverPainter {
    private CoverPainter() {}

    private static final Color[][] COLOR_PALETTES = {
            { new Color(236, 72, 153), new Color(219, 39, 119), new Color(157, 23, 77) }, // Pink
            { new Color(59, 130, 246), new Color(37, 99, 235), new Color(29, 78, 216) },   // Blue
            { new Color(20, 184, 166), new Color(13, 148, 136), new Color(15, 118, 110) }, // Teal
            { new Color(100, 116, 139), new Color(71, 85, 105), new Color(51, 65, 85) },    // Slate/Grey
            { new Color(168, 85, 247), new Color(147, 51, 234), new Color(107, 33, 168) }, // Purple
            { new Color(245, 158, 11), new Color(217, 119, 6), new Color(180, 83, 9) }     // Amber
    };

    public static void paintBanner(Graphics2D g2, int width, int height, String code, String coverPath, int topRadius) {
        Theme.setAntiAliasing(g2);

        Shape clip = new RoundRectangle2D.Float(0, 0, width, height + topRadius, topRadius * 2, topRadius * 2);
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
            paintProceduralCover(g2, width, height, code);
        }

        g2.setClip(oldClip);
    }

    private static void paintProceduralCover(Graphics2D g2, int width, int height, String code) {
        int hash = Math.abs(code != null ? code.hashCode() : 0);
        Color[] palette = COLOR_PALETTES[hash % COLOR_PALETTES.length];

        // Background subtle gradient
        GradientPaint bgGrad = new GradientPaint(0, 0, palette[0], width, height, palette[1]);
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, width, height);

        // Procedural low-poly geometric mesh
        int cols = 5;
        int rows = 3;
        float dx = (float) width / cols;
        float dy = (float) height / rows;

        Point[][] grid = new Point[cols + 1][rows + 1];
        java.util.Random rnd = new java.util.Random(hash);

        for (int i = 0; i <= cols; i++) {
            for (int j = 0; j <= rows; j++) {
                int px = (int) (i * dx);
                int py = (int) (j * dy);
                if (i > 0 && i < cols) {
                    px += (rnd.nextInt(21) - 10);
                }
                if (j > 0 && j < rows) {
                    py += (rnd.nextInt(15) - 7);
                }
                grid[i][j] = new Point(px, py);
            }
        }

        // Draw low-poly triangles
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                Point p1 = grid[i][j];
                Point p2 = grid[i + 1][j];
                Point p3 = grid[i][j + 1];
                Point p4 = grid[i + 1][j + 1];

                // Triangle 1: (p1, p2, p3)
                paintTriangle(g2, p1, p2, p3, palette, rnd);
                // Triangle 2: (p2, p4, p3)
                paintTriangle(g2, p2, p4, p3, palette, rnd);
            }
        }
    }

    private static void paintTriangle(Graphics2D g2, Point p1, Point p2, Point p3, Color[] palette, java.util.Random rnd) {
        GeneralPath path = new GeneralPath();
        path.moveTo(p1.x, p1.y);
        path.lineTo(p2.x, p2.y);
        path.lineTo(p3.x, p3.y);
        path.closePath();

        Color base = palette[rnd.nextInt(palette.length)];
        int alpha = 40 + rnd.nextInt(90);
        g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
        g2.fill(path);

        g2.setColor(new Color(255, 255, 255, 25));
        g2.draw(path);
    }
}
