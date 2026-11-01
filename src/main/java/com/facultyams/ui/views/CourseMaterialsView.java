package com.facultyams.ui.views;

import com.facultyams.model.CourseOffering;
import com.facultyams.model.student.StudentMaterial;
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
    private boolean populatingFilter;
    private List<StudentMaterial> current = new java.util.ArrayList<>();

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
        reload();
    }

    /** Re-reads the enrolled courses and their materials (call when the screen is shown). */
    public void reload() {
        populatingFilter = true;
        Object previous = courseFilter.getSelectedItem();
        courseFilter.removeAllItems();
        courseFilter.addItem("ALL COURSES");
        try {
            for (CourseOffering c : dataService.getEnrolledCourses()) {
                courseFilter.addItem(c.getCode() + " - " + c.getTitle());
            }
        } catch (RuntimeException ex) {
            // the table shows the error
        }
        if (previous != null) courseFilter.setSelectedItem(previous);
        populatingFilter = false;
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
        courseFilter.setFont(Theme.FONT_BODY);
        courseFilter.addActionListener(e -> {
            if (!populatingFilter) refreshMaterials();
        });

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

    /** Folder holding uploaded course files: data/materials/&lt;courseCode&gt;/&lt;fileName&gt;. */
    static java.nio.file.Path materialSource(String courseCode, String fileName) {
        String safeCode = courseCode == null ? "" : courseCode.replaceAll("[^A-Za-z0-9_-]", "_");
        String safeName = fileName == null ? "" : java.nio.file.Path.of(fileName).getFileName().toString();
        return java.nio.file.Path.of("data", "materials", safeCode, safeName);
    }

    /** Copies a stored material to a user-chosen location. */
    static void copyMaterial(java.nio.file.Path source, java.nio.file.Path target) throws java.io.IOException {
        java.nio.file.Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static final java.util.Set<String> OPENABLE = java.util.Set.of("pdf", "doc", "docx", "ppt", "pptx",
            "xls", "xlsx", "txt", "md", "csv", "zip", "png", "jpg", "jpeg", "java");

    /**
     * Opens or saves a material. The backend re-checks that the student is enrolled in the material's course.
     * Links open in the browser (http/https only); file references are only served from the course storage
     * folder and only for document-type extensions; plain-text references are shown in a dialog.
     */
    private void openMaterial(int materialId) {
        StudentMaterial m;
        try {
            m = dataService.getMaterial(materialId);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Material unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (m == null) {
            JOptionPane.showMessageDialog(this, "This material is not available for your courses.",
                    "Material unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String ref = m.getReference();
        if (ref.isEmpty()) {
            showText(m, m.getNotes().isEmpty() ? "No file or link has been attached to this item." : m.getNotes());
        } else if (m.isLink()) {
            try {
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    throw new UnsupportedOperationException("No browser is available");
                }
                Desktop.getDesktop().browse(java.net.URI.create(ref));
            } catch (Exception ex) {
                showText(m, "Open this link in your browser:\n" + ref);
            }
        } else if ("File".equals(m.getType())) {
            downloadMaterial(m);
        } else {
            showText(m, ref);
        }
    }

    private void showText(StudentMaterial m, String text) {
        JTextArea area = new JTextArea(text, 8, 44);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), m.getTitle(), JOptionPane.INFORMATION_MESSAGE);
    }

    private void downloadMaterial(StudentMaterial m) {
        String name = java.nio.file.Path.of(m.getReference().replace('\\', '/')).getFileName().toString();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        java.nio.file.Path source = materialSource(m.getCourseCode(), name);
        if (!OPENABLE.contains(ext) || !java.nio.file.Files.isRegularFile(source)) {
            JOptionPane.showMessageDialog(this,
                    "'" + name + "' is not available in course storage yet.\nExpected location: " + source,
                    "File Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        java.io.File downloads = new java.io.File(System.getProperty("user.home"), "Downloads");
        JFileChooser chooser = new JFileChooser(downloads.isDirectory() ? downloads : new java.io.File(System.getProperty("user.home")));
        chooser.setSelectedFile(new java.io.File(chooser.getCurrentDirectory(), source.getFileName().toString()));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            copyMaterial(source, chooser.getSelectedFile().toPath());
            JOptionPane.showMessageDialog(this, "Saved to " + chooser.getSelectedFile(),
                    "Download Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save the file: " + ex.getMessage(),
                    "Download Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showMessage(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(Theme.FONT_BODY);
        label.setForeground(Theme.TEXT_MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(48, 0, 48, 0));
        tableContainer.removeAll();
        tableContainer.add(label, BorderLayout.CENTER);
        tableContainer.revalidate();
        tableContainer.repaint();
    }

    private void refreshMaterials() {
        String selected = (String) courseFilter.getSelectedItem();
        String codeFilter = "ALL";
        if (selected != null && !selected.equals("ALL COURSES")) {
            codeFilter = selected.split(" - ")[0].trim();
        }
        String search = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        showMessage("Loading materials…");
        final String filter = codeFilter;
        new SwingWorker<List<StudentMaterial>, Void>() {
            @Override protected List<StudentMaterial> doInBackground() {
                return dataService.getMaterialsForCourse(filter);
            }

            @Override protected void done() {
                try {
                    List<StudentMaterial> list = get();
                    if (!search.isEmpty()) {
                        list = list.stream().filter(m ->
                                m.getTitle().toLowerCase().contains(search)
                                        || m.getCourseName().toLowerCase().contains(search)
                                        || m.getCourseCode().toLowerCase().contains(search)
                                        || m.getNotes().toLowerCase().contains(search)
                                        || m.getType().toLowerCase().contains(search)).toList();
                    }
                    current = new java.util.ArrayList<>(list);
                    if (current.isEmpty()) {
                        showMessage(search.isEmpty()
                                ? "There are no materials for your enrolled courses yet."
                                : "No materials match your search.");
                    } else {
                        showTable();
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    showMessage(cause.getMessage() == null ? "Materials could not be loaded." : cause.getMessage());
                }
            }
        }.execute();
    }

    private void showTable() {
        List<StudentMaterial> list = current;
        String[] cols = {"Title", "Course", "Type", "Notes", "Lecturer", "Action"};
        Object[][] data = new Object[list.size()][6];
        for (int i = 0; i < list.size(); i++) {
            StudentMaterial m = list.get(i);
            data[i][0] = m.getTitle();
            data[i][1] = m.getCourseCode();
            data[i][2] = m.getType();
            data[i][3] = m.getNotes();
            data[i][4] = m.getLecturer();
            data[i][5] = m.isLink() ? "Open link" : "Open";
        }

        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 5;
            }
        };

        ModernTable table = new ModernTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(240);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);

        table.getColumnModel().getColumn(2).setCellRenderer((t, value, isSel, hasFocus, r, c) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
            p.setOpaque(false);
            p.add(BadgeLabel.createStatusBadge((String) value));
            return p;
        });

        table.getColumnModel().getColumn(5).setCellRenderer((t, value, isSel, hasFocus, r, c) -> {
            ModernButton btn = new ModernButton(String.valueOf(value), ModernButton.StyleType.OUTLINE);
            btn.setIcon(IconHelper.getIcon("downloads", 14, Theme.PRIMARY));
            btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            return btn;
        });

        table.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            private final ModernButton btn = new ModernButton("Open", ModernButton.StyleType.OUTLINE);
            private int materialId;

            {
                btn.setIcon(IconHelper.getIcon("downloads", 14, Theme.PRIMARY));
                btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
                btn.addActionListener(e -> {
                    fireEditingStopped();
                    openMaterial(materialId);
                });
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
                materialId = current.get(table.convertRowIndexToModel(row)).getId();
                btn.setText(String.valueOf(value));
                return btn;
            }
        });

        tableContainer.removeAll();
        tableContainer.add(ModernTable.createModernScrollPane(table), BorderLayout.CENTER);
        tableContainer.revalidate();
        tableContainer.repaint();
    }
}
