package com.facultyams.ui.views;

import com.facultyams.model.results.ResultGrade;
import com.facultyams.model.results.ResultStatusFilter;
import com.facultyams.service.DataService;
import com.facultyams.service.GradingScheme;
import com.facultyams.service.ResultFilter;
import com.facultyams.service.ResultService;
import com.facultyams.service.ResultService.CourseResult;
import com.facultyams.service.ResultService.SemesterResult;
import com.facultyams.service.ResultService.SemesterStatus;
import com.facultyams.service.ResultService.Summary;
import com.facultyams.ui.components.CardPanel;
import com.facultyams.ui.components.ResultsFilterBar;
import com.facultyams.ui.theme.IconHelper;
import com.facultyams.ui.theme.Theme;
import com.facultyams.util.Constants;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * Displays computed academic results, GPA tools, and performance summaries.
 */
public class StudentResultsView extends JPanel {
    private static final String[] GRADES = {"A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "E"};
    private final ResultService resultService = ResultService.getInstance();
    private final JPanel page = new ResultsPage();
    private final JPanel selectedViewPanel = new JPanel(new BorderLayout());
    private final ButtonGroup viewGroup = new ButtonGroup();
    private final List<YearTab> yearTabs = new ArrayList<>();
    private final ResultsFilterBar resultsFilterBar = new ResultsFilterBar();
    private ResultFilter activeFilter = new ResultFilter("", ResultGrade.ALL, ResultStatusFilter.ALL);
    private JPanel resultsContent;
    private int selectedYear = 0;
    private volatile List<SemesterResult> semesters = List.of();
    private volatile Summary summary = new Summary(BigDecimal.ZERO, 0, 0, 0, BigDecimal.ZERO, 0);
    private final JPanel metricsHolder = new JPanel(new BorderLayout()) {
        @Override public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    };
    private boolean loading = true;
    private String loadError;
    private String selectedView = "Results";
    private int expandedCourseId = -1;

    /** Creates the Results screen. */
    public StudentResultsView() {
        setOpaque(false);
        setLayout(new BorderLayout());
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setOpaque(false);
        page.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
        metricsHolder.setOpaque(false);
        metricsHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(metricsHolder);
        page.add(Box.createVerticalStrut(24));
        page.add(createViewSelector());
        page.add(Box.createVerticalStrut(16));
        selectedViewPanel.setOpaque(false);
        selectedViewPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(selectedViewPanel);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);
        reload();
    }

    /** Reloads results from the database in the background and re-renders every tab. */
    public void reload() {
        loading = true;
        loadError = null;
        renderAll();
        new SwingWorker<List<SemesterResult>, Void>() {
            @Override protected List<SemesterResult> doInBackground() {
                return resultService.getSemesters();
            }
            @Override protected void done() {
                try {
                    List<SemesterResult> loaded = get();
                    semesters = loaded;
                    summary = resultService.summarize(loaded);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    loadError = "Loading was interrupted.";
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    loadError = cause.getMessage() == null ? "Results could not be loaded." : cause.getMessage();
                }
                loading = false;
                renderAll();
            }
        }.execute();
    }

    private void renderAll() {
        metricsHolder.removeAll();
        if (!loading && loadError == null && !semesters.isEmpty()) {
            metricsHolder.add(createSummaryMetrics(), BorderLayout.CENTER);
        }
        metricsHolder.revalidate();
        metricsHolder.repaint();
        showSelectedView();
    }

    private JComponent messagePanel(String text, boolean error, boolean retry) {
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(40, 8, 40, 8));
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_BODY);
        label.setForeground(error ? Theme.ACCENT_ROSE : Theme.TEXT_MUTED);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(label);
        if (retry) {
            JButton again = new JButton("Retry");
            again.setForeground(Theme.PRIMARY);
            again.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
            again.setAlignmentX(Component.CENTER_ALIGNMENT);
            again.addActionListener(e -> reload());
            box.add(Box.createVerticalStrut(8));
            box.add(again);
        }
        return box;
    }

    private String studentLine() {
        try {
            var st = DataService.getInstance().getCurrentStudent();
            String reg = st.getRegNo() == null ? "" : st.getRegNo();
            return st.getFullName() + (reg.isBlank() ? "" : "  ·  " + reg);
        } catch (RuntimeException ex) {
            return "Student";
        }
    }

    private String programLine() {
        try {
            String dept = DataService.getInstance().getCurrentStudent().getDepartment();
            return dept == null || dept.isBlank() ? "" : dept;
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private BigDecimal cumulativeThrough(int overallSemester) {
        List<CourseResult> upTo = new ArrayList<>();
        for (SemesterResult s : semesters) {
            if (s.overallSemester() <= overallSemester) upTo.addAll(s.courses());
        }
        return resultService.calculateGpa(upTo);
    }

    private void exportPdf() {
        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JComboBox<String> scope = new JComboBox<>(new String[]{"All years", "Year 1", "Year 2", "Year 3", "Year 4"});
        JCheckBox includeProvisional = new JCheckBox("Include semesters with pending results");
        options.add(new JLabel("Transcript scope"));
        options.add(scope);
        options.add(includeProvisional);
        if (JOptionPane.showConfirmDialog(this, options, "Transcript options",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        int year = scope.getSelectedIndex();
        boolean includeProvisionalResults = includeProvisional.isSelected();
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("academic-transcript.pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws IOException {
                writeSimplePdf(file, year, includeProvisionalResults);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(StudentResultsView.this, "Transcript PDF exported successfully.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StudentResultsView.this, "Could not export transcript: " + ex.getMessage(),
                            "Export failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void writeSimplePdf(File file, int selectedYear, boolean includeInProgress) throws IOException {
        Summary summary = this.summary;
        List<String> lines = new ArrayList<>();
        lines.add("STUDENT RESULT MANAGEMENT SYSTEM");
        lines.add("Academic Transcript - " + studentLine().replace("  ·  ", " - "));
        lines.add(programLine() + " | Generated " + java.time.LocalDate.now());
        lines.add("CGPA " + summary.cgpa() + " / 4.00 | Credits earned " + summary.earnedCredits()
                + " | Courses graded " + summary.gradedCourses());
        lines.add("");
        for (SemesterResult semester : semesters) {
            if (selectedYear > 0 && semester.year() != selectedYear) continue;
            if (semester.status() == SemesterStatus.IN_PROGRESS && !includeInProgress) continue;
            lines.add("YEAR " + semester.year() + " - SEMESTER " + semester.semester()
                    + " (" + semester.status() + ") | SGPA " + semester.sgpa()
                    + " | Credits " + semester.credits());
            for (CourseResult course : semester.courses()) {
                lines.add(course.code() + "  " + course.name() + "  " + course.credits()
                        + " Cr  " + course.scoreText() + "/" + course.maxScore() + "  "
                        + course.grade() + "  " + course.status());
            }
            lines.add("");
        }
        StringBuilder stream = new StringBuilder("BT\n/F1 9 Tf\n48 760 Td\n");
        int lineCount = 0;
        for (String line : lines) {
            if (lineCount > 0) stream.append("0 -14 Td\n");
            stream.append('(').append(pdfEscape(line)).append(") Tj\n");
            lineCount++;
        }
        stream.append("ET");
        byte[] content = stream.toString().getBytes(StandardCharsets.ISO_8859_1);
        String[] objects = {
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
            "<< /Length " + content.length + " >>\nstream\n" + stream + "\nendstream"
        };
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.length; i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objects[i]).append("\nendobj\n");
        }
        int xrefOffset = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        pdf.append("xref\n0 ").append(objects.length + 1).append("\n0000000000 65535 f \n");
        for (Integer offset : offsets) pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        pdf.append("trailer\n<< /Size ").append(objects.length + 1).append(" /Root 1 0 R >>\n")
                .append("startxref\n").append(xrefOffset).append("\n%%EOF");
        Files.write(file.toPath(), pdf.toString().getBytes(StandardCharsets.ISO_8859_1));
    }

    private String pdfEscape(String text) {
        return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    private JComponent createSummaryMetrics() {
        Summary summary = this.summary;
        int registered = semesters.stream().mapToInt(x -> x.courses().size()).sum();
        long passed = semesters.stream().flatMap(x -> x.courses().stream())
                .filter(c -> c.status() == ResultService.ResultStatus.PASS).count();
        long failed = semesters.stream().flatMap(x -> x.courses().stream())
                .filter(c -> c.status() == ResultService.ResultStatus.FAIL).count();
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(900, 104));
        row.setMinimumSize(new Dimension(0, 104));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        row.add(metricCard("trophy", "Cumulative GPA",
                String.format(Locale.ROOT, "%.2f / %.2f", summary.cgpa(), 4.0),
                String.format(Locale.ROOT, "Latest semester SGPA %.2f", summary.latestSgpa()),
                Theme.ACCENT_BLUE_BG, Theme.PRIMARY));
        row.add(metricCard("trophy", "Courses Graded",
                summary.gradedCourses() + " of " + registered,
                (registered - summary.gradedCourses()) + " awaiting results",
                Theme.ACCENT_AMBER_BG, Theme.ACCENT_AMBER));
        row.add(metricCard("courses", "Credits Earned",
                summary.earnedCredits() + " / " + summary.plannedCredits(),
                "Passed of registered credits",
                Theme.ACCENT_GREEN_BG, Theme.ACCENT_GREEN));
        row.add(metricCard("profile", "Passed / Failed", passed + " / " + failed,
                "Courses with published marks",
                Theme.ACCENT_PURPLE_BG, Theme.ACCENT_PURPLE));
        return row;
    }

    private static final class BadgeRenderer extends DefaultTableCellRenderer {
        private final boolean grade;
        BadgeRenderer(boolean grade) { this.grade = grade; }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                                boolean focus, int row, int column) {
            String text = value == null ? "" : value.toString();
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            label.setOpaque(true);
            label.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            if (grade) {
                Color fg = !Character.isLetter(text.isEmpty() ? '-' : text.charAt(0)) ? Theme.ACCENT_GRAY
                        : text.startsWith("A") ? Theme.ACCENT_GREEN
                        : text.startsWith("B") ? Theme.PRIMARY
                        : text.startsWith("C") ? Theme.ACCENT_AMBER : Theme.ACCENT_ROSE;
                label.setForeground(fg);
                label.setBackground(selected ? Theme.PRIMARY_LIGHT : new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 24));
            } else {
                Color fg = switch (text) {
                    case "Pass" -> Theme.ACCENT_GREEN;
                    case "Fail" -> Theme.ACCENT_ROSE;
                    case "Provisional" -> Theme.PRIMARY;
                    case "Absent" -> Theme.ACCENT_AMBER;
                    default -> Theme.ACCENT_GRAY;
                };
                label.setForeground(fg);
                label.setBackground(selected ? Theme.PRIMARY_LIGHT : new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 24));
            }
            return label;
        }
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        private static final int[] ALIGNMENT = {SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.RIGHT,
                SwingConstants.LEFT, SwingConstants.RIGHT, SwingConstants.CENTER, SwingConstants.RIGHT, SwingConstants.CENTER};
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                                boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setHorizontalAlignment(ALIGNMENT[column]);
            setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
            setForeground(Theme.TEXT_MUTED);
            setBackground(new Color(248, 250, 252));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.TIMETABLE_CARD_BORDER),
                    BorderFactory.createEmptyBorder(0, 16, 0, 16)));
            return this;
        }
    }

    private JComponent metricCard(String iconName, String label, String value, String hint, Color tile, Color ink) {
        CardPanel card = new CardPanel(12);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JPanel icon = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(tile);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        icon.setOpaque(false);
        icon.setPreferredSize(new Dimension(44, 44));
        icon.add(new JLabel(IconHelper.getIcon(iconName, 22, ink)));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(label);
        name.setFont(Theme.FONT_SMALL);
        name.setForeground(Theme.TEXT_MUTED);
        JLabel metric = new JLabel(value);
        metric.setFont(new Font("Segoe UI", Font.BOLD, 22));
        metric.setForeground(Theme.TEXT_MAIN);
        JLabel sub = new JLabel(hint);
        sub.setFont(Theme.FONT_SMALL);
        sub.setForeground(Theme.TEXT_MUTED);
        text.add(name);
        text.add(Box.createVerticalStrut(2));
        text.add(metric);
        text.add(Box.createVerticalStrut(2));
        text.add(sub);
        card.add(icon, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JComponent createViewSelector() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String view : new String[]{"Results", "GPA Calculator", "Performance"}) {
            JToggleButton button = new ViewTab(view);
            viewGroup.add(button);
            row.add(button);
            button.addActionListener(e -> {
                selectedView = view;
                showSelectedView();
            });
            if (view.equals(selectedView)) button.setSelected(true);
        }
        return row;
    }

    private void showSelectedView() {
        selectedViewPanel.removeAll();
        if (loading) {
            selectedViewPanel.add(messagePanel("Loading results…", false, false), BorderLayout.CENTER);
        } else if (loadError != null) {
            selectedViewPanel.add(messagePanel(loadError, true, true), BorderLayout.CENTER);
        } else if (semesters.isEmpty() && !selectedView.equals("GPA Calculator")) {
            selectedViewPanel.add(messagePanel(
                    "No results yet. Results appear here once you are enrolled in courses and marks are published.",
                    false, false), BorderLayout.CENTER);
        } else {
            switch (selectedView) {
                case "GPA Calculator" -> selectedViewPanel.add(new GpaCalculatorPanel(), BorderLayout.CENTER);
                case "Performance" -> selectedViewPanel.add(createPerformance(), BorderLayout.CENTER);
                default -> selectedViewPanel.add(createResultsView(), BorderLayout.CENTER);
            }
        }
        selectedViewPanel.revalidate();
        selectedViewPanel.repaint();
    }

    private JComponent createResultsView() {
        JPanel root = new JPanel();
        root.setOpaque(false);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        JPanel toolbar = createResultsToolbar();
        root.add(toolbar);
        root.add(Box.createVerticalStrut(12));
        resultsContent = new JPanel();
        resultsContent.setOpaque(false);
        resultsContent.setLayout(new BoxLayout(resultsContent, BoxLayout.Y_AXIS));
        root.add(resultsContent);
        refreshResults();
        return root;
    }

    private JPanel createResultsToolbar() {
        JPanel toolbar = new JPanel(new GridBagLayout());
        toolbar.setOpaque(false);
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        tabs.setOpaque(false);
        yearTabs.clear();
        String[] years = {"Year 1", "Year 2", "Year 3", "Year 4", "All Years"};
        for (int i = 0; i < years.length; i++) {
            int year = i == 4 ? 0 : i + 1;
            YearTab tab = new YearTab(years[i], year);
            yearTabs.add(tab);
            tabs.add(tab);
        }
        FlatButton transcript = new FlatButton("Transcript");
        transcript.addActionListener(e -> showTranscriptMenu(transcript));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 8, 8);
        toolbar.add(tabs, c);
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 0, 8, 0);
        toolbar.add(transcript, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        toolbar.add(resultsFilterBar, c);
        resultsFilterBar.addFilterListener(filter -> {
            activeFilter = filter;
            refreshResults();
        });
        return toolbar;
    }

    private void refreshResults() {
        if (resultsContent == null) return;
        resultsContent.removeAll();
        final int year = selectedYear;
        List<SemesterResult> semesters = year == 0
                ? this.semesters : this.semesters.stream().filter(x -> x.year() == year).toList();
        int totalResults = semesters.stream().mapToInt(semester -> semester.courses().size()).sum();
        int visibleResults = (int) semesters.stream().flatMap(semester -> semester.courses().stream())
                .filter(activeFilter::matches).count();
        JLabel resultCount = new JLabel("Showing " + visibleResults + " of " + totalResults + " results");
        resultCount.setFont(Theme.FONT_SMALL);
        resultCount.setForeground(Theme.TEXT_MUTED);
        resultCount.setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 0));
        resultCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultsContent.add(resultCount);
        int currentYearHeading = -1;
        for (SemesterResult semester : semesters) {
            if (selectedYear == 0 && semester.year() != currentYearHeading) {
                currentYearHeading = semester.year();
                JLabel heading = new JLabel("Year " + currentYearHeading);
                heading.setFont(Theme.FONT_SUBTITLE);
                heading.setForeground(Theme.TEXT_MAIN);
                heading.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 0));
                resultsContent.add(heading);
            }
            List<CourseResult> filtered = semester.courses().stream()
                    .filter(activeFilter::matches).toList();
            if (filtered.isEmpty() && semester.status() != SemesterStatus.UPCOMING) continue;
            resultsContent.add(createSemesterSection(semester, filtered));
            resultsContent.add(Box.createVerticalStrut(12));
        }
        if (visibleResults == 0) {
            JPanel empty = new JPanel();
            empty.setOpaque(false);
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            JLabel message = new JLabel("No results match your filters");
            message.setForeground(Theme.TEXT_MUTED);
            message.setFont(Theme.FONT_BODY);
            message.setAlignmentX(Component.CENTER_ALIGNMENT);
            JButton clear = new JButton("Clear filters");
            clear.setForeground(Theme.PRIMARY);
            clear.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
            clear.setBorderPainted(false);
            clear.setContentAreaFilled(false);
            clear.setFocusPainted(false);
            clear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            clear.setAlignmentX(Component.CENTER_ALIGNMENT);
            clear.addActionListener(e -> resultsFilterBar.resetFilters());
            empty.add(message);
            empty.add(Box.createVerticalStrut(8));
            empty.add(clear);
            empty.setBorder(BorderFactory.createEmptyBorder(24, 8, 24, 8));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            empty.setForeground(Theme.TEXT_MUTED);
            resultsContent.add(empty);
        }
        resultsContent.revalidate();
        resultsContent.repaint();
    }

    private JComponent createSemesterSection(SemesterResult semester, List<CourseResult> courses) {
        CardPanel section = new CardPanel(12);
        section.setLayout(new BorderLayout());
        section.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        heading.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Semester " + semester.semester());
        title.setFont(Theme.FONT_SUBTITLE);
        title.setForeground(Theme.TEXT_MAIN);
        JLabel subtitle = new JLabel("Year " + semester.year() + "  ·  Overall Semester "
                + semester.overallSemester());
        subtitle.setFont(Theme.FONT_SMALL);
        subtitle.setForeground(Theme.TEXT_MUTED);
        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(3));
        titlePanel.add(subtitle);
        JLabel status = statusBadge(semester.status());
        JLabel summary = new JLabel(String.format(Locale.ROOT, "SGPA %.2f  ·  %d credits",
                semester.sgpa(), semester.credits()));
        summary.setFont(Theme.FONT_BODY_BOLD);
        summary.setForeground(Theme.TEXT_MAIN);
        JPanel end = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        end.setOpaque(false);
        end.add(status);
        end.add(summary);
        JButton toggle = new JButton(IconHelper.getIcon("chevron-down", 16, Theme.TEXT_MUTED));
        toggle.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        toggle.setContentAreaFilled(false);
        toggle.setFocusPainted(false);
        end.add(toggle);
        heading.add(titlePanel, BorderLayout.WEST);
        heading.add(end, BorderLayout.EAST);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        if (semester.status() == SemesterStatus.UPCOMING) {
            JLabel empty = new JLabel("Results will appear here after the semester ends");
            empty.setFont(Theme.FONT_BODY);
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(20, 18, 20, 18));
            body.add(empty);
        } else if (!courses.isEmpty()) {
            JTable table = createCourseTable(courses);
            JScrollPane tableScroll = new JScrollPane(table);
            tableScroll.setBorder(BorderFactory.createEmptyBorder());
            tableScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            tableScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            tableScroll.getViewport().setOpaque(false);
            tableScroll.setOpaque(false);
            tableScroll.setPreferredSize(new Dimension(900, table.getRowHeight() * (courses.size() + 1)));
            body.add(tableScroll);
            if (expandedCourseId >= 0) {
                CourseResult selected = courses.stream().filter(c -> c.code().hashCode() == expandedCourseId)
                        .findFirst().orElse(null);
                if (selected != null) body.add(createCourseDetail(selected));
            }
            body.add(createSemesterFooter(semester));
        }
        toggle.addActionListener(e -> {
            body.setVisible(!body.isVisible());
            toggle.setIcon(IconHelper.getIcon(body.isVisible() ? "chevron-down" : "chevron-right", 16, Theme.TEXT_MUTED));
            section.revalidate();
        });
        section.add(heading, BorderLayout.NORTH);
        section.add(body, BorderLayout.CENTER);
        return section;
    }

    private JTable createCourseTable(List<CourseResult> courses) {
        String[] columns = {"Course Code", "Subject Name", "Credits", "Assessment", "Score", "Grade", "Grade Points", "Status"};
        Object[][] rows = new Object[courses.size()][columns.length];
        for (int i = 0; i < courses.size(); i++) {
            CourseResult course = courses.get(i);
            rows[i] = new Object[]{course.code(), course.name(), course.credits(), course.assessment(),
                    course.hasMarks() ? course.scoreText() + " / " + course.maxScore() : "—",
                    course.grade(),
                    course.hasMarks() ? String.format(Locale.ROOT, "%.2f", course.gradePoint()) : "—",
                    displayStatus(course)};
        }
        DefaultTableModel model = new DefaultTableModel(rows, columns) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(Theme.FONT_BODY);
        table.setForeground(Theme.TEXT_MAIN);
        table.setBackground(Color.WHITE);
        table.setRowHeight(48);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(238, 242, 247));
        table.setSelectionBackground(Theme.PRIMARY_LIGHT);
        table.setSelectionForeground(Theme.TEXT_MAIN);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
        table.getTableHeader().setForeground(Theme.TEXT_MUTED);
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setReorderingAllowed(false);
        int[] widths = {110, 230, 70, 105, 120, 75, 100, 100};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            int alignment = i == 5 || i == 7 ? SwingConstants.CENTER
                    : i == 2 || i == 4 || i == 6 ? SwingConstants.RIGHT : SwingConstants.LEFT;
            table.getColumnModel().getColumn(i).setCellRenderer(i == 5 || i == 7
                    ? new BadgeRenderer(i == 5) : new CellRenderer(alignment));
        }
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        table.setDefaultEditor(Object.class, null);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int row = table.convertRowIndexToModel(table.getSelectedRow());
                expandedCourseId = courses.get(row).code().hashCode();
                refreshResults();
            }
        });
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "showCourse");
        table.getActionMap().put("showCourse", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (table.getSelectedRow() >= 0) {
                    int row = table.convertRowIndexToModel(table.getSelectedRow());
                    expandedCourseId = courses.get(row).code().hashCode();
                    refreshResults();
                }
            }
        });
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeCourse");
        table.getActionMap().put("closeCourse", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                expandedCourseId = -1;
                showSelectedView();
            }
        });
        return table;
    }

    private JComponent createCourseDetail(CourseResult course) {
        JPanel detail = new JPanel(new BorderLayout(12, 0));
        detail.setOpaque(true);
        detail.setBackground(new Color(248, 250, 252));
        detail.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.TIMETABLE_CARD_BORDER),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));
        JLabel info = new JLabel("<html><b>" + course.code() + " details</b> &nbsp; Marks: "
                + (course.hasMarks() ? course.scoreText() + " / " + course.maxScore() : "not published yet")
                + " &nbsp; · &nbsp; Lecturer: "
                + course.lecturer() + "</html>");
        info.setFont(Theme.FONT_BODY);
        info.setForeground(Theme.TEXT_MAIN);
        JButton add = new JButton("Add to calculator");
        add.setForeground(Theme.PRIMARY);
        add.setBorderPainted(false);
        add.setContentAreaFilled(false);
        add.addActionListener(e -> {
            selectedView = "GPA Calculator";
            showSelectedView();
        });
        detail.add(info, BorderLayout.CENTER);
        detail.add(add, BorderLayout.EAST);
        return detail;
    }

    private JComponent createSemesterFooter(SemesterResult semester) {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.TIMETABLE_CARD_BORDER));
        String creditLabel = "Credits graded";
        JLabel values = new JLabel(String.format(Locale.ROOT, "Semester GPA  %.2f     %s  %d     Cumulative GPA  %.2f",
                semester.sgpa(), creditLabel, semester.credits(),
                cumulativeThrough(semester.overallSemester())));
        values.setFont(Theme.FONT_SMALL);
        values.setForeground(Theme.TEXT_MUTED);
        values.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 8));
        JButton calculator = new JButton("Open in GPA Calculator");
        calculator.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
        calculator.setForeground(Theme.PRIMARY);
        calculator.setContentAreaFilled(false);
        calculator.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        calculator.addActionListener(e -> {
            selectedView = "GPA Calculator";
            showSelectedView();
        });
        footer.add(values, BorderLayout.CENTER);
        footer.add(calculator, BorderLayout.EAST);
        return footer;
    }

    private JLabel statusBadge(SemesterStatus status) {
        String text = switch (status) {
            case COMPLETED -> "Completed";
            case IN_PROGRESS -> "Awaiting results";
            case UPCOMING -> "Upcoming";
        };
        Color bg = switch (status) {
            case COMPLETED -> Theme.ACCENT_GREEN_BG;
            case IN_PROGRESS -> Theme.PRIMARY_LIGHT;
            case UPCOMING -> Theme.ACCENT_GRAY_BG;
        };
        Color fg = switch (status) {
            case COMPLETED -> Theme.ACCENT_GREEN;
            case IN_PROGRESS -> Theme.PRIMARY;
            case UPCOMING -> Theme.ACCENT_GRAY;
        };
        return badge(text, bg, fg);
    }

    private JLabel badge(String text, Color background, Color foreground) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
        label.setForeground(foreground);
        label.setOpaque(true);
        label.setBackground(background);
        label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return label;
    }

    private String displayStatus(CourseResult course) {
        return switch (course.status()) {
            case PASS -> "Pass";
            case FAIL -> "Fail";
            case PENDING -> "Pending";
        };
    }

    private void showTranscriptMenu(Component source) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem preview = new JMenuItem("Preview");
        preview.addActionListener(e -> JOptionPane.showMessageDialog(this,
                transcriptSummary(), "Academic Transcript Preview", JOptionPane.INFORMATION_MESSAGE));
        JMenuItem csv = new JMenuItem("Export CSV");
        csv.addActionListener(e -> exportCsv());
        JMenuItem pdf = new JMenuItem("Save as PDF");
        pdf.addActionListener(e -> exportPdf());
        menu.add(preview);
        menu.add(pdf);
        menu.add(csv);
        menu.show(source, 0, source.getHeight());
    }

    private String transcriptSummary() {
        Summary summary = this.summary;
        return studentLine() + "\n" + programLine() + "\nCGPA: "
                + summary.cgpa() + " / 4.00\nCredits earned: " + summary.earnedCredits()
                + "\nCourses graded: " + summary.gradedCourses();
    }

    private void exportCsv() {
        final List<SemesterResult> snapshot = semesters;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("academic-transcript.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws IOException {
                StringBuilder csv = new StringBuilder("Year,Semester,Course Code,Subject,Credits,Assessment,Score,Grade,Grade Points,Status\n");
                for (SemesterResult semester : snapshot) {
                    for (CourseResult course : semester.courses()) {
                        csv.append(semester.year()).append(',').append(semester.semester()).append(',')
                                .append(csv(course.code())).append(',').append(csv(course.name())).append(',')
                                .append(course.credits()).append(',').append(csv(course.assessment())).append(',')
                                .append(course.hasMarks() ? course.scoreText() : "").append('/').append(course.maxScore()).append(',')
                                .append(course.grade()).append(',').append(course.gradePoint()).append(',')
                                .append(course.status()).append('\n');
                    }
                }
                Files.writeString(file.toPath(), csv.toString(), StandardCharsets.UTF_8);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(StudentResultsView.this, "Transcript exported successfully.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StudentResultsView.this, "Could not export transcript: " + ex.getMessage(),
                            "Export failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private JComponent createPerformance() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        CardPanel chart = new CardPanel(12);
        chart.setLayout(new BorderLayout());
        chart.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel heading = new JLabel("Semester GPA trend");
        heading.setFont(Theme.FONT_SUBTITLE);
        heading.setForeground(Theme.TEXT_MAIN);
        chart.add(heading, BorderLayout.NORTH);
        chart.add(new TrendChart(resultService, semesters), BorderLayout.CENTER);
        chart.setPreferredSize(new Dimension(900, 360));
        chart.setMinimumSize(new Dimension(0, 360));
        chart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
        panel.add(chart);
        panel.add(Box.createVerticalStrut(16));
        Summary summary = this.summary;
        JLabel note = new JLabel("Credits earned: " + summary.earnedCredits() + " / "
                + summary.plannedCredits() + "     ·     Courses graded: " + summary.gradedCourses());
        note.setFont(Theme.FONT_BODY);
        note.setForeground(Theme.TEXT_MUTED);
        panel.add(note);
        return panel;
    }

    private final class YearTab extends JToggleButton {
        private final int year;
        YearTab(String label, int year) {
            super(label);
            this.year = year;
            setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
            setPreferredSize(new Dimension(70, 36));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setSelected(year == selectedYear);
            addActionListener(e -> {
                selectedYear = year;
                for (YearTab tab : yearTabs) tab.setSelected(tab == this);
                refreshResults();
            });
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            g.setColor(isSelected() ? Theme.PRIMARY : (getModel().isRollover() ? Theme.PRIMARY_LIGHT : Color.WHITE));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
            if (!isSelected()) {
                g.setColor(Theme.CONTROL_BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
            }
            g.dispose();
            setForeground(isSelected() ? Color.WHITE : Theme.TEXT_MUTED);
            super.paintComponent(graphics);
        }
    }

    private static final class ViewTab extends JToggleButton {
        ViewTab(String label) {
            super(label);
            setFont(Theme.FONT_BODY_BOLD);
            setPreferredSize(new Dimension(label.equals("GPA Calculator") ? 150 : 112, 40));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            if (isSelected()) {
                g.setColor(Theme.PRIMARY);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
            } else {
                g.setColor(getModel().isRollover() ? Theme.PRIMARY_LIGHT : Color.WHITE);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
                g.setColor(Theme.CONTROL_BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
            }
            g.dispose();
            setForeground(isSelected() ? Color.WHITE : Theme.TEXT_MUTED);
            super.paintComponent(graphics);
        }
    }

    private static final class FlatButton extends JButton {
        FlatButton(String text) {
            super(text);
            setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
            setForeground(Theme.PRIMARY);
            setPreferredSize(new Dimension(96, 36));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createLineBorder(Theme.CONTROL_BORDER));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    private static final class CellRenderer extends DefaultTableCellRenderer {
        private final int alignment;
        CellRenderer(int alignment) { this.alignment = alignment; }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                                boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, false, row, column);
            setHorizontalAlignment(alignment);
            setFont(Theme.FONT_BODY);
            setForeground(Theme.TEXT_MAIN);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(238, 242, 247)),
                    BorderFactory.createEmptyBorder(0, 16, 0, 16)));
            setBackground(selected ? Theme.PRIMARY_LIGHT : Color.WHITE);
            return this;
        }
    }

    private static final class PlaceholderField extends JTextField {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setAntiAliasing(g);
                g.setColor(Theme.TEXT_SUBTLE);
                g.setFont(getFont());
                g.drawString("Search courses...", 10, (getHeight() + g.getFontMetrics().getAscent()) / 2 - 2);
                g.dispose();
            }
        }
    }

    private static final class ResultsPage extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return Math.max(24, visibleRect.height - 24); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static final class TrendChart extends JPanel {
        private final List<SemesterResult> semesters;
        private final ResultService resultService;
        TrendChart(ResultService resultService, List<SemesterResult> semesters) {
            this.resultService = resultService;
            this.semesters = semesters;
            setOpaque(false);
            setPreferredSize(new Dimension(800, 280));
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setAntiAliasing(g);
            int left = 48, right = getWidth() - 20, top = 20, bottom = getHeight() - 40;
            g.setColor(new Color(226, 232, 240));
            for (int i = 0; i <= 4; i++) {
                int y = bottom - (bottom - top) * i / 4;
                g.drawLine(left, y, right, y);
                g.setColor(Theme.TEXT_MUTED);
                g.drawString(String.format(Locale.ROOT, "%.1f", (double) i), 8, y + 4);
                g.setColor(new Color(226, 232, 240));
            }
            g.setColor(Theme.PRIMARY);
            g.setStroke(new BasicStroke(2.2f));
            Point previous = null;
            Point previousCumulative = null;
            for (SemesterResult semester : semesters) {
                int x = left + (right - left) * (semester.overallSemester() - 1) / 7;
                if (semester.credits() == 0) {
                    g.setColor(Theme.TEXT_SUBTLE);
                    g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1, new float[]{5, 5}, 0));
                    previous = null;
                    previousCumulative = null;
                } else {
                    int y = bottom - (int) Math.round((bottom - top) * semester.sgpa().doubleValue() / 4.0);
                    Point current = new Point(x, y);
                    g.setColor(Theme.PRIMARY);
                    g.setStroke(new BasicStroke(2.2f));
                    if (previous != null) g.drawLine(previous.x, previous.y, current.x, current.y);
                    g.fillOval(x - 4, y - 4, 8, 8);
                    g.setColor(Theme.TEXT_MUTED);
                    g.drawString("S" + semester.overallSemester(), x - 8, bottom + 20);
                    previous = current;
                    List<CourseResult> upTo = new ArrayList<>();
                    for (SemesterResult other : semesters) {
                        if (other.overallSemester() <= semester.overallSemester()) upTo.addAll(other.courses());
                    }
                    BigDecimal cgpa = resultService.calculateGpa(upTo);
                    int cumulativeY = bottom - (int) Math.round((bottom - top) * cgpa.doubleValue() / 4.0);
                    Point cumulative = new Point(x, cumulativeY);
                    g.setColor(Theme.ACCENT_GREEN);
                    g.setStroke(new BasicStroke(1.8f));
                    if (previousCumulative != null) {
                        g.drawLine(previousCumulative.x, previousCumulative.y, cumulative.x, cumulative.y);
                    }
                    g.fillOval(x - 3, cumulativeY - 3, 6, 6);
                    previousCumulative = cumulative;
                }
            }
            g.dispose();
        }
    }
}
