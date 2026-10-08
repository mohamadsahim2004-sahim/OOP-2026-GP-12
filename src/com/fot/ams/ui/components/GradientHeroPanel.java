package com.fot.ams.ui.components;

import com.fot.ams.ui.theme.ThemeColors;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/**
 * GradientHeroPanel recreates the striking mesh-gradient left banner
 * seen in the reference design with high-fidelity Java2D painting.
 */
public class GradientHeroPanel extends JPanel {
    private final int cornerRadius;

    public GradientHeroPanel(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        // Clip to rounded rectangle
        RoundRectangle2D roundedCard = new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius);
        g2.setClip(roundedCard);

        // 1. Primary Diagonal Gradient (Deep Sapphire Blue -> Vibrant Violet -> Soft Purple)
        Point2D start = new Point2D.Float(0, h);
        Point2D end = new Point2D.Float(w, 0);
        float[] fractions = {0.0f, 0.45f, 0.85f, 1.0f};
        Color[] colors = {
            ThemeColors.HERO_DEEP_BLUE,
            ThemeColors.HERO_ROYAL_PURPLE,
            ThemeColors.HERO_VIOLET,
            new Color(175, 120, 245)
        };
        LinearGradientPaint baseGradient = new LinearGradientPaint(start, end, fractions, colors);
        g2.setPaint(baseGradient);
        g2.fillRect(0, 0, w, h);

        // 2. Cyan Glowing Aura Bloom at top-left
        Point2D cyanCenter = new Point2D.Float(w * 0.15f, h * 0.18f);
        float cyanRadius = Math.max(w, h) * 0.65f;
        float[] cyanFractions = {0.0f, 0.4f, 1.0f};
        Color[] cyanColors = {
            ThemeColors.HERO_CYAN_BLOOM,
            new Color(86, 192, 245, 60),
            new Color(86, 192, 245, 0)
        };
        RadialGradientPaint cyanBloom = new RadialGradientPaint(cyanCenter, cyanRadius, cyanFractions, cyanColors);
        g2.setPaint(cyanBloom);
        g2.fillRect(0, 0, w, h);

        // 3. Violet Highlight Glow at center-right
        Point2D violetCenter = new Point2D.Float(w * 0.85f, h * 0.40f);
        float violetRadius = Math.max(w, h) * 0.7f;
        float[] violetFractions = {0.0f, 0.5f, 1.0f};
        Color[] violetColors = {
            new Color(192, 132, 252, 120),
            new Color(168, 85, 247, 40),
            new Color(168, 85, 247, 0)
        };
        RadialGradientPaint violetBloom = new RadialGradientPaint(violetCenter, violetRadius, violetFractions, violetColors);
        g2.setPaint(violetBloom);
        g2.fillRect(0, 0, w, h);

        // 4. Draw Brand Emblem (White 8-point Asterisk / Star) at top-left
        drawAsterisk(g2, 42, 48, 16, Color.WHITE, 3.5f);

        // 5. Draw Hero Texts at the bottom matching reference image
        int paddingLeft = 38;
        int textY = h - 145;

        // Subtitle "You can easily"
        g2.setColor(new Color(255, 255, 255, 210));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        g2.drawString("Faculty of Technology • Ruhuna", paddingLeft, textY);

        // Large Headline
        textY += 28;
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2.drawString("Get access your personal", paddingLeft, textY);

        textY += 28;
        g2.drawString("hub for clarity and", paddingLeft, textY);

        textY += 28;
        g2.drawString("productivity", paddingLeft, textY);

        // Bottom subtle system tagline
        textY += 26;
        g2.setColor(new Color(255, 255, 255, 160));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.drawString("Academic Management System (ICT2132)", paddingLeft, textY);

        g2.dispose();
    }

    /**
     * Draws a crisp multi-arm asterisk icon matching the reference logo.
     */
    public static void drawAsterisk(Graphics2D g2, int cx, int cy, int radius, Color color, float strokeWidth) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int arms = 8;
        for (int i = 0; i < arms; i++) {
            double angle = i * (Math.PI / 4.0);
            int x1 = cx - (int) Math.round(Math.cos(angle) * radius);
            int y1 = cy - (int) Math.round(Math.sin(angle) * radius);
            int x2 = cx + (int) Math.round(Math.cos(angle) * radius);
            int y2 = cy + (int) Math.round(Math.sin(angle) * radius);
            g2.drawLine(x1, y1, x2, y2);
        }
    }
}
