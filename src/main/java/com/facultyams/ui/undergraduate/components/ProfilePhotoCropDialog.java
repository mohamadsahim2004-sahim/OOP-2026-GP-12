package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Validates an uploaded photo and provides a zoomable, pannable circular crop preview. */
public final class ProfilePhotoCropDialog {
    private ProfilePhotoCropDialog() { }

    /** Opens a file chooser and crop dialog; returns null when the user cancels. */
    public static BufferedImage chooseAndCrop(Component parent) throws IOException {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose profile photo");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("Image files (PNG, JPG, JPEG)", "png", "jpg", "jpeg"));
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        File file = chooser.getSelectedFile();
        if (file.length() > 5L * 1024 * 1024) throw new IOException("Photo must be 5 MB or smaller.");
        BufferedImage source = ImageIO.read(file);
        if (source == null) throw new IOException("The selected file is not a readable PNG or JPEG image.");
        CropDialog dialog = new CropDialog(SwingUtilities.getWindowAncestor(parent), source);
        dialog.setVisible(true);
        return dialog.result;
    }

    private static final class CropDialog extends JDialog {
        private final CropPreview preview;
        private BufferedImage result;

        private CropDialog(Window owner, BufferedImage source) {
            super(owner, "Crop profile photo", ModalityType.APPLICATION_MODAL);
            setLayout(new BorderLayout(12, 12));
            JPanel content = new JPanel(new BorderLayout(12, 12));
            content.setBackground(Color.WHITE);
            content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            preview = new CropPreview(source);
            content.add(preview, BorderLayout.CENTER);
            JSlider zoom = new JSlider(100, 250, 120);
            zoom.setBorder(BorderFactory.createTitledBorder("Zoom"));
            zoom.addChangeListener(e -> preview.setZoom(zoom.getValue() / 100.0));
            content.add(zoom, BorderLayout.SOUTH);
            add(content, BorderLayout.CENTER);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
            actions.setBackground(Color.WHITE);
            JButton cancel = new FlatButton("Cancel", false);
            cancel.addActionListener(e -> dispose());
            JButton apply = new FlatButton("Apply", true);
            apply.addActionListener(e -> {
                result = ImageUtils.cropAvatar(source, preview.zoom, preview.offsetX, preview.offsetY, 512);
                dispose();
            });
            actions.add(cancel);
            actions.add(apply);
            add(actions, BorderLayout.SOUTH);
            setSize(360, 430);
            setLocationRelativeTo(owner);
        }
    }

    private static final class CropPreview extends JPanel {
        private final BufferedImage source;
        private double zoom = 1.2;
        private double offsetX;
        private double offsetY;
        private Point dragStart;

        private CropPreview(BufferedImage source) {
            this.source = source;
            setOpaque(false);
            setPreferredSize(new Dimension(280, 280));
            MouseAdapter drag = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { dragStart = e.getPoint(); }
                @Override public void mouseDragged(MouseEvent e) {
                    if (dragStart == null) return;
                    double sourcePerPixel = Math.min(source.getWidth(), source.getHeight()) / zoom / 220.0;
                    offsetX -= (e.getX() - dragStart.x) * sourcePerPixel;
                    offsetY -= (e.getY() - dragStart.y) * sourcePerPixel;
                    dragStart = e.getPoint();
                    repaint();
                }
            };
            addMouseListener(drag);
            addMouseMotionListener(drag);
        }

        private void setZoom(double zoom) {
            this.zoom = zoom;
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            int diameter = Math.min(getWidth(), getHeight()) - 16;
            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;
            double side = Math.min(source.getWidth(), source.getHeight()) / zoom;
            double sx = Math.max(0, Math.min(source.getWidth() - side, (source.getWidth() - side) / 2 + offsetX));
            double sy = Math.max(0, Math.min(source.getHeight() - side, source.getHeight() * 0.04 + offsetY));
            g.setClip(new java.awt.geom.Ellipse2D.Double(x, y, diameter, diameter));
            g.drawImage(source, x, y, x + diameter, y + diameter,
                    (int) sx, (int) sy, (int) (sx + side), (int) (sy + side), null);
            g.setClip(null);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(4));
            g.drawOval(x, y, diameter, diameter);
            g.dispose();
        }
    }
}
