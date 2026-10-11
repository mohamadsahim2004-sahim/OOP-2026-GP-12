package com.facultyams.ui.views;

import com.facultyams.model.Course;
import com.facultyams.model.CourseMaterial;
import com.facultyams.service.DataService;
import com.facultyams.ui.components.*;
import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CourseMaterialsView extends JPanel {
    private final DataService dataService = DataService.getInstance();
    private JComboBox<String> courseFilter;
    private JTextField searchField;
    private JPanel tableContainer;

    public CourseMaterialsView() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(Theme.createEmptyPadding(0, 0, 24, 0));

        // Header Card
        content.add(createHeaderCard());
        content.add(Box.createVerticalStrut(18));

        // Filter Bar & Table Card
        content.add(createMaterialsCard());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);
        refreshMaterials();
    }

    private JPanel createHeaderCard() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(20, 0));
        card.setBorder(Theme.createEmptyPadding(20, 24, 20, 24));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Course Materials & Lecture Resources");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel sub = new JLabel("Access lecture slides, tutorial sheets, lab manuals, and past examination papers.");
        sub.setFont(Theme.FONT_BODY);
        sub.setForeground(Theme.TEXT_MUTED);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        card.add(left, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMaterialsCard() {
        CardPanel card = new CardPanel(16);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(Theme.createEmptyPadding(18, 20, 18, 20));

        // Filter Row
        JPanel filterRow = new JPanel(new BorderLayout(14, 0));
        filterRow.setOpaque(false);

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftFilters.setOpaque(false);

        JLabel filterLbl = new JLabel("Filter Course:");
        filterLbl.setFont(Theme.FONT_BODY_BOLD);
        filterLbl.setForeground(Theme.TEXT_MAIN);

        courseFilter = new JComboBox<>();
        courseFilter.addItem("ALL COURSES");
        for (Course c : dataService.getEnrolledCourses()) {
            courseFilter.addItem(c.getCode() + " - " + c.getTitle());
        }
        courseFilter.setFont(Theme.FONT_BODY);
        courseFilter.addActionListener(e -> refreshMaterials());

        leftFilters.add(filterLbl);
        leftFilters.add(courseFilter);

        // Search box right
        JPanel rightSearch = new JPanel(new BorderLayout(6, 0));
        rightSearch.setOpaque(false);
        searchField = new JTextField(16);
        searchField.setFont(Theme.FONT_BODY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        searchField.addActionListener(e -> refreshMaterials());

        ModernButton searchBtn = new ModernButton("Search", ModernButton.StyleType.PRIMARY);
        searchBtn.addActionListener(e -> refreshMaterials());

        rightSearch.add(searchField, BorderLayout.CENTER);
        rightSearch.add(searchBtn, BorderLayout.EAST);

        filterRow.add(leftFilters, BorderLayout.WEST);
        filterRow.add(rightSearch, BorderLayout.EAST);
        card.add(filterRow, BorderLayout.NORTH);

        tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);
        card.add(tableContainer, BorderLayout.CENTER);

        return card;
    }

    private void refreshMaterials() {
        String selected = (String) courseFilter.getSelectedItem();
        String codeFilter = "ALL";
        if (selected != null && !selected.equals("ALL COURSES")) {
            codeFilter = selected.split(" - ")[0].trim();
        }

        String search = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        List<CourseMaterial> list = dataService.getMaterialsForCourse(codeFilter);

        // Apply text search
        if (!search.isEmpty()) {
            list = list.stream().filter(m -> 
                m.getTitle().toLowerCase().contains(search) || 
                m.getCourseName().toLowerCase().contains(search) ||
                m.getType().toLowerCase().contains(search)
            ).toList();
        }

        String[] cols = {"File Name", "Course", "Category", "Size", "Date", "Uploaded By", "Action"};
        Object[][] data = new Object[list.size()][7];

        for (int i = 0; i < list.size(); i++) {
            CourseMaterial m = list.get(i);
            data[i][0] = m.getTitle();
            data[i][1] = m.getCourseCode();
            data[i][2] = m.getType();
            data[i][3] = m.getFileSize();
            data[i][4] = m.getUploadDate();
            data[i][5] = m.getUploadedBy();
            data[i][6] = "Download";
        }

        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 6;
            }
        };

        ModernTable table = new ModernTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(260);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);

        table.getColumnModel().getColumn(2).setCellRenderer((t, value, isSel, hasFocus, r, c) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
            p.setOpaque(false);
            p.add(BadgeLabel.createStatusBadge((String) value));
            return p;
        });

        table.getColumnModel().getColumn(6).setCellRenderer((t, value, isSel, hasFocus, r, c) -> {
            ModernButton btn = new ModernButton("Download", ModernButton.StyleType.OUTLINE);
            btn.setIcon(IconHelper.getIcon("downloads", 14, Theme.PRIMARY));
            btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            return btn;
        });

        table.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            private final ModernButton btn = new ModernButton("Download", ModernButton.StyleType.OUTLINE);
            private String fileName;

            {
                btn.setIcon(IconHelper.getIcon("downloads", 14, Theme.PRIMARY));
                btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
                btn.addActionListener(e -> {
                    fireEditingStopped();
                    JOptionPane.showMessageDialog(CourseMaterialsView.this,
                            "Downloading '" + fileName + "'...\nSaved to your Downloads folder successfully!",
                            "Download Complete", JOptionPane.INFORMATION_MESSAGE);
                });
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
                fileName = (String) table.getValueAt(row, 0);
                return btn;
            }
        });

        tableContainer.removeAll();
        tableContainer.add(ModernTable.createModernScrollPane(table), BorderLayout.CENTER);
        tableContainer.revalidate();
        tableContainer.repaint();
    }
}
