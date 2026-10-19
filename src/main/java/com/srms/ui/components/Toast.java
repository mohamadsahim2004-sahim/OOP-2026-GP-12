package com.srms.ui.components;

import com.srms.Theme;
import java.awt.*;
import javax.swing.*;

/**
 * Clean floating toast notification that fades out after a specified duration (e.g. 3s).
 */
public class Toast extends JPanel {
    private final JLabel messageLbl = new JLabel();
    private float alpha = 1.0f;
    private Timer fadeTimer;
    private Timer holdTimer;

    public Toast() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 14, 8));
        setOpaque(false);
        setVisible(false);

        messageLbl.setFont(Theme.FONT_BODY_BOLD);
        messageLbl.setForeground(Color.WHITE);
        add(messageLbl);
    }

    public static void show(JComponent parent, String message) {
        show(parent, message, 3000);
    }

    public static void show(JComponent parent, String message, int durationMs) {
        if (parent.getRootPane() == null) return;
        final JLayeredPane layeredPane = parent.getRootPane().getLayeredPane();
        if (layeredPane == null) return;

        Toast toast = new Toast();
        toast.messageLbl.setText(message);
        Dimension pref = toast.getPreferredSize();
        int w = Math.max(pref.width + 30, 220);
        int h = 42;
        int x = (layeredPane.getWidth() - w) / 2;
        int y = layeredPane.getHeight() - h - 50;

        toast.setBounds(x, y, w, h);
        layeredPane.add(toast, JLayeredPane.POPUP_LAYER);
        toast.setVisible(true);

        toast.holdTimer = new Timer(durationMs, e -> toast.startFade(layeredPane));
        toast.holdTimer.setRepeats(false);
        toast.holdTimer.start();
    }

    private void startFade(JLayeredPane parent) {
        fadeTimer = new Timer(30, e -> {
            alpha -= 0.05f;
            if (alpha <= 0.0f) {
                fadeTimer.stop();
                parent.remove(this);
                parent.repaint();
            } else {
                repaint();
            }
        });
        fadeTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setAntiAliasing(g2);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.0f, Math.min(1.0f, alpha))));

        // Dark modern pill background
        g2.setColor(new Color(15, 23, 42, 230));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

        // Subtle green check dot
        g2.setColor(Theme.SUCCESS);
        g2.fillOval(14, (getHeight() - 10) / 2, 10, 10);

        g2.dispose();
        super.paintComponent(g);
    }
}
