package com.srms.ui.components;

import com.srms.Theme;
import com.srms.Ui;
import com.srms.model.CourseStatus;
import com.srms.model.SortOption;
import com.srms.model.ViewMode;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Filter controls that publish a combined immutable state to observers. */
public class CourseToolbar extends JPanel {
    public record FilterState(String searchText, CourseStatus status, SortOption sortOption, ViewMode viewMode) {}

    private final StyledComboBox<CourseStatus> status = new StyledComboBox<>(CourseStatus.values());
    private final StyledComboBox<SortOption> sort = new StyledComboBox<>(SortOption.values());
    private final StyledTextField search = new StyledTextField("Search courses");
    private final JButton clear = new JButton("×");
    private final JButton reset = new JButton("Reset");
    private final JButton grid = new JButton("▦");
    private final JButton list = new JButton("☰");
    private final List<Consumer<FilterState>> listeners = new ArrayList<>();
    private final Timer debounce;
    private ViewMode viewMode = ViewMode.GRID;

    public CourseToolbar() {
        setOpaque(false);
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        debounce = new Timer(200, e -> notifyListeners());
        debounce.setRepeats(false);

        JPanel searchBox = buildSearch();
        JPanel viewToggle = buildViewToggle();
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 12);

        c.gridx = 0;
        c.weightx = 0;
        status.setPreferredSize(new Dimension(160, 40));
        add(status, c);

        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        searchBox.setPreferredSize(new Dimension(240, 40));
        add(searchBox, c);

        c.gridx++;
        c.weightx = 0;
        sort.setPreferredSize(new Dimension(180, 40));
        add(sort, c);

        c.gridx++;
        c.insets = new Insets(0, 0, 0, 12);
        add(viewToggle, c);

        c.gridx++;
        c.insets = new Insets(0, 0, 0, 0);
        add(reset, c);

        reset.setVisible(false);
        styleReset();
        status.addActionListener(e -> notifyListeners());
        sort.addActionListener(e -> notifyListeners());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onSearchChange(); }
            @Override public void removeUpdate(DocumentEvent e) { onSearchChange(); }
            @Override public void changedUpdate(DocumentEvent e) { onSearchChange(); }
        });
        reset.addActionListener(e -> clearFilters());
        setViewMode(ViewMode.GRID);
    }

    public void addFilterListener(Consumer<FilterState> listener) {
        if (listener != null) listeners.add(listener);
    }

    public FilterState getFilterState() {
        return new FilterState(search.getText().trim(),
                (CourseStatus) status.getSelectedItem(),
                (SortOption) sort.getSelectedItem(),
                viewMode);
    }

    public void clearFilters() {
        search.setText("");
        status.setSelectedItem(CourseStatus.ALL);
        sort.setSelectedItem(SortOption.SHORT_NAME);
        updateViewButtons();
        reset.setVisible(false);
        debounce.stop();
        notifyListeners();
    }

    private JPanel buildSearch() {
        JPanel box = new JPanel(new BorderLayout(8, 0)) {
            private boolean focused;
            {
                setOpaque(true);
                setBackground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 8));
                search.addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                    @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g2 = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g2);
                g2.setColor(focused ? Theme.PRIMARY : Theme.CARD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        JLabel icon = new JLabel("⌕");
        icon.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        icon.setForeground(Theme.TEXT_MUTED);
        box.add(icon, BorderLayout.WEST);
        search.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        box.add(search, BorderLayout.CENTER);
        clear.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        clear.setForeground(Theme.TEXT_MUTED);
        clear.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        clear.setContentAreaFilled(false);
        clear.setFocusPainted(false);
        clear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clear.setVisible(false);
        clear.addActionListener(e -> search.setText(""));
        box.add(clear, BorderLayout.EAST);
        return box;
    }

    private JPanel buildViewToggle() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 0, 0));
        panel.setOpaque(false);
        styleToggleButton(grid);
        styleToggleButton(list);
        grid.addActionListener(e -> setViewMode(ViewMode.GRID));
        list.addActionListener(e -> setViewMode(ViewMode.LIST));
        panel.add(grid);
        panel.add(list);
        panel.setPreferredSize(new Dimension(88, 40));
        return panel;
    }

    private void styleToggleButton(JButton button) {
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER));
        button.setPreferredSize(new Dimension(44, 40));
    }

    private void setViewMode(ViewMode mode) {
        viewMode = mode;
        updateViewButtons();
        if (grid != null) notifyListeners();
    }

    private void updateViewButtons() {
        styleActive(grid, viewMode == ViewMode.GRID);
        styleActive(list, viewMode == ViewMode.LIST);
    }

    private void styleActive(JButton button, boolean active) {
        button.setBackground(active ? Theme.PRIMARY_LIGHT : Color.WHITE);
        button.setForeground(active ? Theme.PRIMARY : Theme.TEXT_MUTED);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createLineBorder(active ? Theme.PRIMARY_BORDER : Theme.CARD_BORDER));
    }

    private void styleReset() {
        reset.setFont(Theme.FONT_BODY);
        reset.setForeground(Theme.PRIMARY);
        reset.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        reset.setContentAreaFilled(false);
        reset.setFocusPainted(false);
        reset.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        reset.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { reset.setText("<html><u>Reset</u></html>"); }
            @Override public void mouseExited(MouseEvent e) { reset.setText("Reset"); }
        });
    }

    private void onSearchChange() {
        boolean hasText = !search.getText().isEmpty();
        clear.setVisible(hasText);
        reset.setVisible(hasText || status.getSelectedItem() != CourseStatus.ALL
                || sort.getSelectedItem() != SortOption.SHORT_NAME);
        debounce.restart();
    }

    private void notifyListeners() {
        FilterState state = getFilterState();
        reset.setVisible(!state.searchText().isEmpty() || state.status() != CourseStatus.ALL
                || state.sortOption() != SortOption.SHORT_NAME);
        for (Consumer<FilterState> listener : List.copyOf(listeners)) listener.accept(state);
    }
}
