package com.srms.ui.components;

import com.srms.Theme;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Custom modern underline tab bar matching minimal design standards.
 */
public class UnderlineTabs extends JPanel {
    private final List<String> tabTitles = new ArrayList<>();
    private final List<JLabel> tabLabels = new ArrayList<>();
    private final Consumer<Integer> onTabChanged;
    private int selectedIndex = 0;

    public UnderlineTabs(List<String> titles, Consumer<Integer> onTabChanged) {
        this.tabTitles.addAll(titles);
        this.onTabChanged = onTabChanged;

        setLayout(new FlowLayout(FlowLayout.LEFT, 24, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        initUI();
    }

    private void initUI() {
        for (int i = 0; i < tabTitles.size(); i++) {
            final int index = i;
            JLabel lbl = new JLabel(tabTitles.get(i)) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (index == selectedIndex) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        Theme.setAntiAliasing(g2);
                        g2.setColor(Theme.PRIMARY);
                        int lineY = getHeight() - 2;
                        g2.fillRect(0, lineY, getWidth(), 2);
                        g2.dispose();
                    }
                }
            };
            lbl.setFont(Theme.FONT_BODY_BOLD);
            lbl.setForeground(index == selectedIndex ? Theme.PRIMARY : Theme.TEXT_MUTED);
            lbl.setBorder(BorderFactory.createEmptyBorder(6, 4, 10, 4));
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            lbl.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setSelectedIndex(index);
                }
            });

            tabLabels.add(lbl);
            add(lbl);
        }
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < tabTitles.size()) {
            this.selectedIndex = index;
            for (int i = 0; i < tabLabels.size(); i++) {
                JLabel l = tabLabels.get(i);
                l.setForeground(i == selectedIndex ? Theme.PRIMARY : Theme.TEXT_MUTED);
                l.repaint();
            }
            if (onTabChanged != null) {
                onTabChanged.accept(selectedIndex);
            }
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }
}
