package com.facultyams.ui.technical;

import com.facultyams.exception.ValidationException;
import com.facultyams.model.CourseInfo;
import com.facultyams.model.ReportResult;
import com.facultyams.service.AttendanceService;
import com.facultyams.ui.admin.AdminTheme;
import com.facultyams.util.CsvWriter;
import com.facultyams.util.PdfWriter;
import com.facultyams.util.UIUtil;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Template for a report page: common filters (student, course, batch, from, to), Generate, a summary line, a
 * chart, a sortable table, Print, Export CSV and Export PDF. A concrete report supplies its extra filters and
 * what it generates; everything shown, printed and exported comes from the same {@link ReportResult}.
 */
abstract class AbstractReportPanel extends TechnicalPage {

    private static final long serialVersionUID = 1L;

    private record Lookups(List<CourseInfo> courses, List<String> batches) {
    }

    private final transient AttendanceService lookupService = new AttendanceService();
    protected final HintField studentField = new HintField("Student name or reg no", 14);
    protected final JComboBox<Object> courseBox = new JComboBox<>();
    protected final JComboBox<Object> batchBox = new JComboBox<>();
    protected final HintField fromField = new HintField("From (yyyy-MM-dd)", 9);
    protected final HintField toField = new HintField("To (yyyy-MM-dd)", 9);
    protected final JPanel filters = filterBar();

    private final BarChartPanel chart = new BarChartPanel();
    private final JLabel summary = new JLabel(" ");
    private final JTable table = new JTable();
    private final JScrollPane tableScroll = new JScrollPane(table);
    private final String fileStem;
    private transient ReportResult last;
    private boolean fillingLookups;

    protected AbstractReportPanel(String fileStem) {
        this.fileStem = fileStem;
        filters.add(studentField);
        filters.add(labeled("Course", courseBox));
        filters.add(labeled("Batch", batchBox));
        filters.add(fromField);
        filters.add(toField);
        studentField.addActionListener(e -> generate());
        fromField.addActionListener(e -> generate());
        toField.addActionListener(e -> generate());
    }

    /** Called by the subclass constructor once its own filters have been added to {@link #filters}. */
    protected final void buildLayout() {
        JButton generate = button("Generate", "Create the report with the chosen filters", KeyEvent.VK_G, this::generate);
        JButton print = button("Print", "Print the report", KeyEvent.VK_P, this::print);
        JButton csv = button("Export CSV", "Save the report as a CSV file (opens in Excel)", KeyEvent.VK_C, this::exportCsv);
        JButton pdf = button("Export PDF", "Save the report as a PDF file", KeyEvent.VK_F, this::exportPdf);
        JPanel actions = filterBar();
        actions.add(generate);
        actions.add(print);
        actions.add(csv);
        actions.add(pdf);

        summary.setFont(AdminTheme.FONT_BOLD);
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(filters);
        top.add(actions);
        top.add(statusArea());
        top.add(summary);

        JPanel chartRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chartRow.add(chart);
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(chartRow, BorderLayout.NORTH);
        center.add(tableScroll, BorderLayout.CENTER);
        alignLeft(top);
        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }

    /** Reads the filter controls (on the screen thread) and returns the work that builds the report. */
    protected abstract Callable<ReportResult> prepare() throws ValidationException;

    @Override
    public void refresh() {
        load("Loading filters...", () -> new Lookups(lookupService.getCourses(), lookupService.getBatches()), l -> {
            fillingLookups = true;
            fillCombo(courseBox, "All courses", l.courses());
            fillCombo(batchBox, "All batches", l.batches());
            fillingLookups = false;
            generate();
        });
    }

    protected final CourseInfo selectedCourse() {
        return selectedOf(courseBox, CourseInfo.class);
    }

    protected final String selectedBatch() {
        return batchBox.getSelectedIndex() > 0 ? String.valueOf(batchBox.getSelectedItem()) : null;
    }

    /** "Student: ... | Course: ... | From: ..." describing every filter that is set. */
    protected String describeFilters(String... extra) {
        List<String> parts = new ArrayList<>();
        if (!studentField.getText().isBlank()) {
            parts.add("Student: " + studentField.getText().trim());
        }
        if (selectedCourse() != null) {
            parts.add("Course: " + selectedCourse().code());
        }
        if (selectedBatch() != null) {
            parts.add("Batch: " + selectedBatch());
        }
        if (!fromField.getText().isBlank()) {
            parts.add("From: " + fromField.getText().trim());
        }
        if (!toField.getText().isBlank()) {
            parts.add("To: " + toField.getText().trim());
        }
        for (String e : extra) {
            if (e != null) {
                parts.add(e);
            }
        }
        return parts.isEmpty() ? "none (all records)" : String.join("  |  ", parts);
    }

    private void generate() {
        if (fillingLookups) {
            return;
        }
        Callable<ReportResult> work;
        try {
            work = prepare();
        } catch (ValidationException e) {
            showError(e.getMessage());
            return;
        }
        load("Generating the report...", work, this::show);
    }

    private void show(ReportResult r) {
        last = r;
        List<String> headers = r.table().headers();
        ReadOnlyTableModel model = ReadOnlyTableModel.ofStrings(headers.toArray(new String[0]));
        for (List<String> row : r.table().rows()) {
            model.addRow(row.toArray());
        }
        table.setModel(model);
        TableRowSorter<ReadOnlyTableModel> sorter = new TableRowSorter<>(model);
        for (int c = 0; c < headers.size(); c++) {
            sorter.setComparator(c, NUMERIC_AWARE);
        }
        table.setRowSorter(sorter);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int c = 0; c < headers.size(); c++) {
            int max = headers.get(c).length();
            for (List<String> row : r.table().rows()) {
                max = Math.max(max, row.get(c).length());
            }
            TableColumn col = table.getColumnModel().getColumn(c);
            col.setPreferredWidth(Math.max(60, Math.min(260, max * 8 + 24)));
            if (headers.get(c).equals("Status") || headers.get(c).equals("Eligible")) {
                col.setCellRenderer(new StatusRenderer());
            }
        }
        summary.setText(r.summary());
        chart.setData(r.chartTitle(), r.chart());
        showInfo(r.table().rows().isEmpty() ? "No records match these filters." : r.table().rows().size() + " rows.");
    }

    private static final Comparator<Object> NUMERIC_AWARE = (a, b) -> {
        Double x = number(String.valueOf(a));
        Double y = number(String.valueOf(b));
        return x != null && y != null ? Double.compare(x, y) : String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
    };

    private static Double number(String s) {
        try {
            return Double.parseDouble(s.endsWith("%") ? s.substring(0, s.length() - 1) : s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ print and export

    private boolean haveReport() {
        if (last == null) {
            showError("Generate the report first.");
            return false;
        }
        return true;
    }

    private void print() {
        if (!haveReport()) {
            return;
        }
        try {
            table.print(JTable.PrintMode.FIT_WIDTH, new MessageFormat(last.table().title()), new MessageFormat("Page {0}"));
        } catch (PrinterException | java.awt.HeadlessException e) {
            showError("Printing failed: " + e.getMessage());
        }
    }

    private void exportCsv() {
        export("csv", "CSV file", () -> CsvWriter.toCsv(last.table()).getBytes(StandardCharsets.UTF_8));
    }

    private void exportPdf() {
        export("pdf", "PDF file", () -> PdfWriter.toPdf(last.table()));
    }

    private void export(String ext, String description, Callable<byte[]> content) {
        if (!haveReport()) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save " + description);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(description, ext));
        chooser.setSelectedFile(new File(fileStem + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "." + ext));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith("." + ext)) {
            file = new File(file.getParentFile(), file.getName() + "." + ext);
        }
        if (file.exists() && !UIUtil.confirm(this, file.getName() + " already exists. Replace it?")) {
            return;
        }
        try {
            Files.write(file.toPath(), content.call());
            showSuccess("Saved " + file.getAbsolutePath());
        } catch (IOException e) {
            showError("The file could not be saved: " + e.getMessage());
        } catch (Exception e) {
            showError("The export failed: " + e.getMessage());
        }
    }
}