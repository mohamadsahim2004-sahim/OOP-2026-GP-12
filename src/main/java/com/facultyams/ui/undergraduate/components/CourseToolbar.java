package com.facultyams.ui.components;

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

import com.facultyams.Theme;
import com.facultyams.model.CourseStatus;
import com.facultyams.model.SortOption;
import com.facultyams.model.ViewMode;

/** Filter controls that publish a combined immutable state to observers. */
public class CourseToolbar extends JPanel {
    public record FilterState(String searchText, CourseStatus status, SortOption sortOption, ViewMode viewMode) {}

    private final StyledComboBox<CourseStatus> status = new StyledComboBox<>(CourseStatus.values(), "Status: ");
    private final StyledComboBox<SortOption> sort = new StyledComboBox<>(SortOption.values());
    private final StyledTextField search = new StyledTextField("Search courses");
    private final IconButton clear = new IconButton(Icons.Type.CLOSE, "Clear search");
    private final JButton reset = new JButton("Reset");
    private final SegmentedToggle viewToggle = new SegmentedToggle();
    private final List<Consumer<FilterState>> listeners = new ArrayList<>();
    private final Timer debounce;
    private JPanel searchBox;
    private boolean syncing;

    public CourseToolbar() {
        setOpaque(false);
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        debounce = new Timer(200, e -> notifyListeners());
        debounce.setRepeats(false);

        searchBox = buildSearch();
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, Theme.CONTROL_GAP);

        c.gridx = 0;
        c.weightx = 0;
        status.setPreferredSize(new Dimension(180, Theme.CONTROL_HEIGHT));
        status.setMinimumSize(new Dimension(180, Theme.CONTROL_HEIGHT));
        add(status, c);

        c.gridx++;
        c.weightx = 1;
        searchBox.setPreferredSize(new Dimension(240, Theme.CONTROL_HEIGHT));
        searchBox.setMinimumSize(new Dimension(160, Theme.CONTROL_HEIGHT));
        add(searchBox, c);

        c.gridx++;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, Theme.CONTROL_GAP);
        sort.setPreferredSize(new Dimension(200, Theme.CONTROL_HEIGHT));
        sort.setMinimumSize(new Dimension(200, Theme.CONTROL_HEIGHT));
        add(sort, c);

        c.gridx++;
        c.insets = new Insets(0, 0, 0, 0);
        add(viewToggle, c);

        status.addActionListener(e -> { if (!syncing) notifyListeners(); });
        sort.addActionListener(e -> { if (!syncing) notifyListeners(); });
        viewToggle.setViewModeListener(mode -> { if (!syncing) notifyListeners(); });
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onSearchChange(); }
            @Override public void removeUpdate(DocumentEvent e) { onSearchChange(); }
            @Override public void changedUpdate(DocumentEvent e) { onSearchChange(); }
        });
        clear.addActionListener(e -> search.setText(""));
        reset.addActionListener(e -> clearFilters());
    }

    public void addFilterListener(Consumer<FilterState> listener) {
        if (listener != null) listeners.add(listener);
    }

    public FilterState getFilterState() {
        return new FilterState(search.getText().trim(),
                (CourseStatus) status.getSelectedItem(),
                (SortOption) sort.getSelectedItem(),
                viewToggle.getSelectedMode());
    }

    public void clearFilters() {
        syncing = true;
        debounce.stop();
        search.setText("");
        status.setSelectedItem(CourseStatus.ALL);
        sort.setSelectedItem(SortOption.SHORT_NAME);
        viewToggle.setSelectedMode(ViewMode.GRID);
        syncing = false;
        clear.setVisible(false);
        reset.setVisible(false);
        notifyListeners();
    }

    private JPanel buildSearch() {
        JPanel box = new JPanel(new BorderLayout(8, 0)) {
            private boolean focused;
            {
                setOpaque(true);
                setBackground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
                search.addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                    @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(focused ? Theme.PRIMARY : Theme.CARD_BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
                g.dispose();
            }
        };
        box.add(new VectorIcon(Icons.Type.SEARCH, Theme.COURSE_TEXT_MUTED), BorderLayout.WEST);
        search.setBorder(BorderFactory.createEmptyBorder());
        box.add(search, BorderLayout.CENTER);

        JPanel trailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        trailing.setOpaque(false);
        reset.setFont(Theme.COURSE_BODY);
        reset.setForeground(Theme.PRIMARY);
        reset.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        reset.setContentAreaFilled(false);
        reset.setFocusPainted(false);
        reset.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        reset.setVisible(false);
        clear.setVisible(false);
        trailing.add(reset);
        trailing.add(clear);
        box.add(trailing, BorderLayout.EAST);
        return box;
    }

    private void onSearchChange() {
        boolean hasText = !search.getText().isEmpty();
        clear.setVisible(hasText);
        updateResetVisibility();
        if (!syncing) debounce.restart();
    }

    private void updateResetVisibility() {
        reset.setVisible(!search.getText().trim().isEmpty()
                || status.getSelectedItem() != CourseStatus.ALL
                || sort.getSelectedItem() != SortOption.SHORT_NAME);
    }

    private void notifyListeners() {
        FilterState state = getFilterState();
        updateResetVisibility();
        for (Consumer<FilterState> listener : List.copyOf(listeners)) listener.accept(state);
    }

    private static final class VectorIcon extends JComponent {
        private final Icons.Type type;
        private final Color color;
        VectorIcon(Icons.Type type, Color color) {
            this.type = type;
            this.color = color;
            setPreferredSize(new Dimension(24, Theme.CONTROL_HEIGHT));
        }
        @Override protected void paintComponent(Graphics graphics) {
            int size = Math.min(18, getHeight() - 4);
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            Icons.paint((Graphics2D) graphics, type, x, y, size, color);
        }
    }

    private static final class IconButton extends JButton {
        private final Icons.Type type;
        private boolean hovered;
        IconButton(Icons.Type type, String description) {
            this.type = type;
            setToolTipText(description);
            setPreferredSize(new Dimension(24, 28));
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            if (hovered) {
                g.setColor(Theme.HOVER_BG);
                g.fillOval(1, 1, getWidth() - 2, getHeight() - 2);
            }
            Icons.paint(g, type, (getWidth() - 14) / 2, (getHeight() - 14) / 2,
                    14, Theme.COURSE_TEXT_MUTED);
            g.dispose();
        }
    }
}
