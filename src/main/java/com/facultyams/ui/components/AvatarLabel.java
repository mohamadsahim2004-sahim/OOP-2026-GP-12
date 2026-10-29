package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.swing.JComponent;

/** Circular student avatar with a graceful initials fallback. */
public final class AvatarLabel extends JComponent {
    private String imagePath;
    private final String initials;
    private final int diameter;
    private BufferedImage image;

    /** Creates a round avatar with the supplied dimensions and fallback initials. */
    public AvatarLabel(String imagePath, String initials, int diameter) {
        this.imagePath = imagePath;
        this.initials = initials;
        this.diameter = diameter;
        setOpaque(false);
        setPreferredSize(new Dimension(diameter + 8, diameter + 8));
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        refreshImage();
    }

    /** Updates the photo source and refreshes the scaled-image cache. */
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
        ImageUtils.clearCache();
        refreshImage();
    }

    private void refreshImage() {
        try {
            image = imagePath == null || imagePath.isBlank() ? null : ImageUtils.getAvatarImage(imagePath, diameter);
        } catch (IOException ex) {
            image = null;
        }
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setAntiAliasing(g);
        int x = (getWidth() - diameter) / 2;
        int y = (getHeight() - diameter) / 2;
        g.setColor(new Color(15, 23, 42, 24));
        g.fillOval(x + 1, y + 3, diameter, diameter);
        Shape circle = new java.awt.geom.Ellipse2D.Double(x, y, diameter, diameter);
        g.setClip(circle);
        if (image != null) {
            g.drawImage(image, x, y, diameter, diameter, null);
        } else {
            g.setPaint(new GradientPaint(x, y, Theme.PRIMARY, x + diameter, y + diameter, new Color(99, 102, 241)));
            g.fill(circle);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, Math.max(12, diameter / 3)));
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(initials, x + (diameter - metrics.stringWidth(initials)) / 2,
                    y + (diameter - metrics.getHeight()) / 2 + metrics.getAscent());
        }
        g.setClip(null);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f));
        g.draw(circle);
        g.dispose();
    }
}
