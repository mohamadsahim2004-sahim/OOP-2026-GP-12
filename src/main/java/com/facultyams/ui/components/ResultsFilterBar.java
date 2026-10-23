package com.facultyams.ui.components;

import com.facultyams.model.results.ResultGrade;
import com.facultyams.model.results.ResultStatusFilter;
import com.facultyams.service.ResultFilter;
import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Responsive search, grade, and status filter controls for the Results screen. */
public final class ResultsFilterBar extends JPanel {
    private final JTextField search = new PlaceholderField();
    private final JComboBox<ResultGrade> grade = new JComboBox<>(ResultGrade.values());
    private final JComboBox<ResultStatusFilter> status = new JComboBox<>(ResultStatusFilter.values());
    private final JButton clearSearch = new ClearIconButton();
    private final JButton reset = new JButton("Reset");
    private final javax.swing.Timer debounce;
    private final List<Consumer<ResultFilter>> listeners = new ArrayList<>();

    public ResultsFilterBar() {
        setOpaque(false);
        setLayout(new GridBagLayout());
        debounce = new javax.swing.Timer(200, e -> fireFilterChanged());
        debounce.setRepeats(false);

        JPanel surface = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(Color.WHITE);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g.setColor(new Color(229, 231, 235));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        surface.setOpaque(false);
        surface.setBorder(new EmptyBorder(12, 16, 12, 16));
        surface.setPreferredSize(new Dimension(700, 64));
        surface.setMinimumSize(new Dimension(0, 64));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 12);

        JComponent searchControl = createSearchControl();
        c.gridx = 0;
        c.weightx = 1;
        surface.add(searchControl, c);

        styleCombo(grade, false);
        c.gridx = 1;
        c.weightx = 0;
        c.ipadx = 0;
        surface.add(grade, c);

        styleCombo(status, true);
        c.gridx = 2;
        surface.add(status, c);

        reset.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        reset.setForeground(Theme.PRIMARY);
        reset.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        reset.setContentAreaFilled(false);
        reset.setFocusPainted(false);
        reset.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        reset.setVisible(false);
        reset.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { reset.setText("<html><u>Reset</u></html>"); }
            @Override public void mouseExited(MouseEvent e) { reset.setText("Reset"); }
        });
        reset.addActionListener(e -> resetFilters());
        c.gridx = 3;
        c.insets = new Insets(0, 0, 0, 0);
        surface.add(reset, c);

        GridBagConstraints outer = new GridBagConstraints();
        outer.gridx = 0;
        outer.gridy = 0;
        outer.weightx = 1;
        outer.fill = GridBagConstraints.HORIZONTAL;
        add(surface, outer);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onSearchChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onSearchChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onSearchChanged(); }
        });
        clearSearch.addActionListener(e -> search.setText(""));
        grade.addActionListener(e -> fireFilterChanged());
        status.addActionListener(e -> fireFilterChanged());
        syncClearButton();
    }

    /** Registers a listener invoked whenever any filter changes. */
    public void addFilterListener(Consumer<ResultFilter> listener) {
        listeners.add(listener);
    }

    /** Current combined filter state. */
    public ResultFilter getFilter() {
        return new ResultFilter(search.getText(), (ResultGrade) grade.getSelectedItem(),
                (ResultStatusFilter) status.getSelectedItem());
    }

    private JComponent createSearchControl() {
        JPanel control = new JPanel(new BorderLayout(8, 0));
        control.setOpaque(true);
        control.setBackground(Color.WHITE);
        control.setBorder(new RoundedFieldBorder(search));
        control.setPreferredSize(new Dimension(280, 40));
        control.setMinimumSize(new Dimension(240, 40));
        JLabel icon = new JLabel(IconHelper.getIcon("search", 16, Theme.TEXT_MUTED));
        icon.setBorder(new EmptyBorder(0, 10, 0, 0));
        search.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        search.setForeground(new Color(17, 24, 39));
        search.setCaretColor(Theme.PRIMARY);
        search.setOpaque(false);
        search.setBorder(new EmptyBorder(0, 0, 0, 0));
        search.setMinimumSize(new Dimension(100, 36));
        search.setPreferredSize(new Dimension(190, 36));
        search.putClientProperty("placeholderText", "Search courses...");
        clearSearch.setVisible(false);
        control.add(icon, BorderLayout.WEST);
        control.add(search, BorderLayout.CENTER);
        control.add(clearSearch, BorderLayout.EAST);
        return control;
    }

    private void styleCombo(JComboBox<?> combo, boolean isStatus) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setForeground(new Color(17, 24, 39));
        combo.setBackground(Color.WHITE);
        combo.setUI(new RoundedComboUI());
        combo.setOpaque(false);
        combo.setBorder(new RoundedComboBorder());
        combo.setRenderer(new FilterRenderer(combo, isStatus));
        combo.setPreferredSize(new Dimension(160, 40));
        combo.setMinimumSize(new Dimension(160, 40));
        combo.setMaximumSize(new Dimension(160, 40));
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        combo.setFocusable(true);
        combo.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { combo.putClientProperty("filterHover", Boolean.TRUE); combo.repaint(); }
            @Override public void mouseExited(MouseEvent e) { combo.putClientProperty("filterHover", Boolean.FALSE); combo.repaint(); }
        });
    }

    private void onSearchChanged() {
        syncClearButton();
        reset.setVisible(getFilter().isActive());
        debounce.restart();
    }

    private void syncClearButton() {
        clearSearch.setVisible(!search.getText().isEmpty());
        revalidate();
    }

    private void fireFilterChanged() {
        ResultFilter filter = getFilter();
        reset.setVisible(filter.isActive());
        for (Consumer<ResultFilter> listener : List.copyOf(listeners)) {
            listener.accept(filter);
        }
    }

    public void resetFilters() {
        debounce.stop();
        search.setText("");
        grade.setSelectedItem(ResultGrade.ALL);
        status.setSelectedItem(ResultStatusFilter.ALL);
        reset.setVisible(false);
        fireFilterChanged();
    }

    private static final class RoundedFieldBorder extends AbstractBorder {
        private final JTextField field;
        private RoundedFieldBorder(JTextField field) { this.field = field; }
        @Override public Insets getBorderInsets(Component c) { return new Insets(2, 2, 2, 2); }
        @Override public void paintBorder(Component c, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(field.isFocusOwner() ? Theme.PRIMARY : new Color(229, 231, 235));
            g.setStroke(new BasicStroke(field.isFocusOwner() ? 1.5f : 1f));
            g.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
            g.dispose();
        }
    }

    private static final class RoundedComboBorder extends AbstractBorder {
        @Override public Insets getBorderInsets(Component component) {
            return new Insets(2, 10, 2, 28);
        }
        @Override public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            boolean emphasized = component.hasFocus()
                    || Boolean.TRUE.equals(((JComponent) component).getClientProperty("filterHover"));
            g.setColor(emphasized ? Theme.PRIMARY : new Color(229, 231, 235));
            g.setStroke(new BasicStroke(emphasized ? 1.5f : 1f));
            g.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
            g.dispose();
        }
    }

    private static final class ClearIconButton extends JButton {
        private ClearIconButton() {
            setToolTipText("Clear search");
            setPreferredSize(new Dimension(32, 32));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            if (getModel().isRollover()) {
                g.setColor(Theme.PRIMARY_LIGHT);
                g.fillOval(3, 3, getWidth() - 6, getHeight() - 6);
            }
            g.setColor(Theme.TEXT_MUTED);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            g.drawLine(centerX - 4, centerY - 4, centerX + 4, centerY + 4);
            g.drawLine(centerX + 4, centerY - 4, centerX - 4, centerY + 4);
            g.dispose();
        }
    }

    private static final class FilterRenderer extends JPanel implements ListCellRenderer<Object> {
        private final JComboBox<?> combo;
        private final JLabel dot = new JLabel();
        private final JLabel text = new JLabel();
        private final JLabel check = new JLabel();
        private final boolean status;

        private FilterRenderer(JComboBox<?> combo, boolean status) {
            this.combo = combo;
            this.status = status;
            setLayout(new BorderLayout(8, 0));
            setBorder(new EmptyBorder(7, 10, 7, 10));
            setOpaque(true);
            add(dot, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
            add(check, BorderLayout.EAST);
        }

        @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                 boolean selected, boolean focus) {
            boolean isStatus = value instanceof ResultStatusFilter;
            String label = value instanceof ResultGrade item ? item.label()
                    : value instanceof ResultStatusFilter item ? item.label() : String.valueOf(value);
            text.setText(label);
            text.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            check.setText(value != null && value.equals(combo.getSelectedItem()) ? "✓" : "");
            check.setForeground(Theme.PRIMARY);
            dot.setIcon(null);
            dot.setText("");
            if (status && value instanceof ResultStatusFilter item && item.color() != null) {
                dot.setIcon(new StatusDot(item.color()));
            }
            setBackground(selected ? Theme.PRIMARY_LIGHT : Color.WHITE);
            text.setForeground(selected ? Theme.PRIMARY : new Color(17, 24, 39));
            return this;
        }
    }

    private static final class StatusDot implements Icon {
        private final Color color;
        private StatusDot(Color color) { this.color = color; }
        @Override public int getIconWidth() { return 8; }
        @Override public int getIconHeight() { return 8; }
        @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(color);
            g.fillOval(x, y + 2, 7, 7);
            g.dispose();
        }
    }

    private static final class RoundedComboUI extends BasicComboBoxUI {
        @Override protected JButton createArrowButton() {
            JButton button = new JButton() {
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    Theme.setAntiAliasing(g);
                    g.setColor(isEnabled() ? new Color(75, 85, 99) : Theme.TEXT_SUBTLE);
                    Path2D arrow = new Path2D.Float();
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    arrow.moveTo(cx - 4, cy - 2);
                    arrow.lineTo(cx, cy + 2);
                    arrow.lineTo(cx + 4, cy - 2);
                    g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(arrow);
                    g.dispose();
                }
            };
            button.setBorder(BorderFactory.createEmptyBorder());
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            button.setOpaque(false);
            button.setPreferredSize(new Dimension(28, 36));
            return button;
        }

        @Override protected ComboPopup createPopup() {
            ComboPopup popup = super.createPopup();
            if (popup instanceof JPopupMenu menu) {
                menu.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(229, 231, 235)),
                        new EmptyBorder(3, 0, 3, 0)));
                menu.setBackground(Color.WHITE);
            }
            popup.getList().setBackground(Color.WHITE);
            popup.getList().setSelectionBackground(Theme.PRIMARY_LIGHT);
            popup.getList().setSelectionForeground(Theme.PRIMARY);
            return popup;
        }

        @Override public void configureEditor() {
            super.configureEditor();
            if (comboBox.getEditor().getEditorComponent() instanceof JComponent editor) {
                editor.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 4));
                editor.setOpaque(false);
            }
        }

        @Override public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds, boolean hasFocus) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(Color.WHITE);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            g.dispose();
        }
    }

    private static final class PlaceholderField extends JTextField {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(new Color(107, 114, 128));
                g.setFont(getFont());
                Insets insets = getInsets();
                String placeholder = String.valueOf(getClientProperty("placeholderText"));
                g.drawString(placeholder, insets.left + 1,
                        (getHeight() + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2);
                g.dispose();
            }
        }
    }
}
