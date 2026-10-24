package com.facultyams.ui.components;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import com.facultyams.Theme;
import com.facultyams.Ui;

/**
 * CollapsibleSection card component with smooth animation, round chevron button,
 * 18px bold title, optional "Collapse all / Expand all" header action, and activity rows.
 */
public class CollapsibleSection extends Ui.Card {
    private final String sectionId;
    private final String title;
    private final boolean showCollapseAllLink;
    private final Runnable onToggleAll;

    private boolean expanded = true;
    private final JPanel contentWrapper = new JPanel(new BorderLayout());
    private final JPanel activitiesList = new JPanel();
    private final List<ActivityRow> activityRows = new ArrayList<>();
    private final ChevronButton chevronButton = new ChevronButton();
    private JLabel toggleAllLabel;

    private Timer animTimer;
    private int currentContentHeight = -1;
    private int targetContentHeight = -1;

    public CollapsibleSection(String sectionId, String title, boolean showCollapseAllLink, Runnable onToggleAll) {
        super(12, Theme.CARD_BG, Theme.CARD_BORDER);
        this.sectionId = sectionId;
        this.title = title;
        this.showCollapseAllLink = showCollapseAllLink;
        this.onToggleAll = onToggleAll;

        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        // Section Header (~56px high)
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel west = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        west.setOpaque(false);
        west.add(chevronButton);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.FONT_SECTION); // bold 18px
        titleLbl.setForeground(Theme.TEXT_MAIN);
        west.add(titleLbl);
        header.add(west, BorderLayout.WEST);

        if (showCollapseAllLink) {
            toggleAllLabel = new JLabel("Collapse all");
            toggleAllLabel.setFont(Theme.FONT_SMALL);
            toggleAllLabel.setForeground(Theme.PRIMARY);
            toggleAllLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            toggleAllLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (onToggleAll != null) onToggleAll.run();
                }
            });
            header.add(toggleAllLabel, BorderLayout.EAST);
        }

        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Ignore click if clicking the toggle all link
                if (showCollapseAllLink && toggleAllLabel != null &&
                        toggleAllLabel.getBounds().contains(SwingUtilities.convertPoint(header, e.getPoint(), toggleAllLabel.getParent()))) {
                    return;
                }
                toggle();
            }
        });

        add(header, BorderLayout.NORTH);

        // Content Area
        activitiesList.setLayout(new BoxLayout(activitiesList, BoxLayout.Y_AXIS));
        activitiesList.setOpaque(false);

        // 1px header divider
        JPanel headerDivider = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.CARD_BORDER);
                g.drawLine(0, 0, getWidth(), 0);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(0, 1);
            }
        };

        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.add(headerDivider, BorderLayout.NORTH);
        inner.add(activitiesList, BorderLayout.CENTER);

        contentWrapper.setOpaque(false);
        contentWrapper.add(inner, BorderLayout.CENTER);
        add(contentWrapper, BorderLayout.CENTER);
    }

    public void addActivityRow(ActivityRow row) {
        activityRows.add(row);
        activitiesList.add(row);
    }

    public void updateAllStatus() {
        for (ActivityRow r : activityRows) {
            r.updateStatus();
        }
    }

    public void setToggleAllText(String text) {
        if (toggleAllLabel != null) {
            toggleAllLabel.setText(text);
        }
    }

    public void setExpanded(boolean exp, boolean animate) {
        if (this.expanded == exp) return;
        this.expanded = exp;
        chevronButton.setExpanded(exp);

        int fullH = activitiesList.getPreferredSize().height + 1; // +1 for divider

        if (!animate) {
            if (exp) {
                contentWrapper.setPreferredSize(null);
                contentWrapper.setVisible(true);
            } else {
                contentWrapper.setPreferredSize(new Dimension(0, 0));
                contentWrapper.setVisible(false);
            }
            revalidate();
            repaint();
            return;
        }

        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }

        contentWrapper.setVisible(true);
        int startH = contentWrapper.getHeight();
        if (exp && startH <= 0) startH = 0;
        targetContentHeight = exp ? fullH : 0;
        currentContentHeight = startH;

        int step = Math.max(16, Math.abs(targetContentHeight - currentContentHeight) / 10);

        animTimer = new Timer(15, e -> {
            if (expanded) {
                currentContentHeight = Math.min(targetContentHeight, currentContentHeight + step);
            } else {
                currentContentHeight = Math.max(targetContentHeight, currentContentHeight - step);
            }
            contentWrapper.setPreferredSize(new Dimension(getWidth(), currentContentHeight));
            contentWrapper.revalidate();
            Container parent = getParent();
            if (parent != null) {
                parent.revalidate();
            }

            if (currentContentHeight == targetContentHeight) {
                animTimer.stop();
                if (!expanded) {
                    contentWrapper.setVisible(false);
                } else {
                    contentWrapper.setPreferredSize(null);
                }
                revalidate();
                repaint();
            }
        });
        animTimer.start();
    }

    public void toggle() {
        setExpanded(!expanded, true);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public String getSectionId() {
        return sectionId;
    }

    /**
     * Circular button with painted chevron arrow.
     */
    private static class ChevronButton extends JComponent {
        private boolean isExp = true;

        public ChevronButton() {
            setPreferredSize(new Dimension(28, 28));
            setOpaque(false);
        }

        public void setExpanded(boolean exp) {
            this.isExp = exp;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setAntiAliasing(g2);

            // Subtle round background
            g2.setColor(new Color(241, 245, 249));
            g2.fillOval(2, 2, 24, 24);

            // Chevron arrow
            g2.setColor(Theme.TEXT_MUTED);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int cx = 14;
            int cy = 14;
            if (isExp) {
                // Down arrow ⌵
                g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
            } else {
                // Right arrow >
                g2.drawLine(cx - 2, cy - 4, cx + 2, cy);
                g2.drawLine(cx + 2, cy, cx - 2, cy + 4);
            }

            g2.dispose();
        }
    }
}
