package com.facultyams.ui.technical;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.service.TechnicalMedicalService;
import com.facultyams.ui.admin.AdminTheme;
import com.facultyams.ui.admin.Refreshable;
import com.facultyams.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Base class of every Technical Officer page. It provides what all pages need so the pages only describe
 * their own content:
 * <ul>
 *   <li>background loading with a "Loading..." state and an inline error state (nothing blocks the window)</li>
 *   <li>inline success / info / error messages in one place</li>
 *   <li>one-action-at-a-time guard for saves and decisions, with a dialog for failures</li>
 *   <li>shared helpers: tables, combo boxes with an "All" entry, date parsing, opening medical documents</li>
 * </ul>
 */
abstract class TechnicalPage extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final JLabel notice = new JLabel(" ");
    private final JTextArea errorText = AdminTheme.errorText();
    private final JPanel statusArea = new JPanel(new BorderLayout());
    private int loadGeneration;
    private boolean actionRunning;

    /** Used by every page that opens a medical document. */
    protected final transient TechnicalMedicalService medicalService = new TechnicalMedicalService();

    protected TechnicalPage() {
        super(new BorderLayout(0, 8));
        notice.setFont(AdminTheme.FONT_BOLD);
        notice.setForeground(AdminTheme.TEXT_MUTED);
        notice.getAccessibleContext().setAccessibleName("Status message");
        errorText.setRows(2);
        errorText.setLineWrap(true);
        errorText.setWrapStyleWord(true);
        errorText.setVisible(false);
        statusArea.add(notice, BorderLayout.NORTH);
        statusArea.add(errorText, BorderLayout.CENTER);
    }

    /** The place where the page shows its messages; the page puts it somewhere in its own layout. */
    protected final JPanel statusArea() {
        return statusArea;
    }

    // ------------------------------------------------------------------ messages

    protected final void showSuccess(String message) {
        setMessage(message, AdminTheme.SUCCESS);
    }

    protected final void showInfo(String message) {
        setMessage(message, AdminTheme.TEXT_MUTED);
    }

    protected final void showError(String message) {
        notice.setText(" ");
        errorText.setText(message);
        errorText.setVisible(true);
        revalidate();
    }

    protected final void clearMessages() {
        setMessage(" ", AdminTheme.TEXT_MUTED);
    }

    private void setMessage(String message, java.awt.Color color) {
        errorText.setText("");
        errorText.setVisible(false);
        notice.setForeground(color);
        notice.setText(message == null || message.isEmpty() ? " " : message);
        revalidate();
    }

    // ------------------------------------------------------------------ background work

    /**
     * Runs {@code work} off the screen thread and passes its result to {@code done}. Only the newest load of
     * a page may update the screen (an older, slower one is dropped). A failure is shown inline.
     */
    protected final <T> void load(String busyText, Callable<T> work, Consumer<T> done) {
        final int mine = ++loadGeneration;
        showInfo(busyText);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                if (mine != loadGeneration) {
                    return;
                }
                try {
                    T result = get();
                    clearMessages();
                    done.accept(result);
                } catch (Exception e) {
                    fail(unwrap(e), false);
                }
            }
        }.execute();
    }

    /**
     * Runs a user action (save, submit, approve ...) once at a time. A second click while it is running is
     * ignored. A failure is shown inline and in a dialog so it cannot be missed.
     */
    protected final <T> void act(String busyText, Callable<T> work, Consumer<T> done) {
        if (actionRunning) {
            return;
        }
        actionRunning = true;
        showInfo(busyText);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                actionRunning = false;
                try {
                    T result = get();
                    clearMessages();
                    done.accept(result);
                } catch (Exception e) {
                    fail(unwrap(e), true);
                }
            }
        }.execute();
    }

    private static Exception unwrap(Exception e) {
        Throwable t = e instanceof java.util.concurrent.ExecutionException && e.getCause() != null ? e.getCause() : e;
        return t instanceof Exception ex ? ex : new RuntimeException(t);
    }

    private void fail(Exception e, boolean dialog) {
        if (e instanceof ValidationException || e instanceof AuthorizationException) {
            showError(e.getMessage());
        } else if (e instanceof DatabaseException) {
            System.err.println("Database error: " + e.getMessage());
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            showError(e.getMessage());
        } else {
            e.printStackTrace();
            showError("Unexpected error: " + e.getMessage());
        }
        if (dialog) {
            UIUtil.showException(this, e);
        }
    }

    // ------------------------------------------------------------------ shared helpers

    protected static JButton button(String text, String tooltip, int mnemonic, Runnable action) {
        JButton b = new JButton(text);
        b.setToolTipText(tooltip);
        b.getAccessibleContext().setAccessibleDescription(tooltip);
        if (mnemonic != 0) {
            b.setMnemonic(mnemonic);
        }
        b.addActionListener(e -> action.run());
        return b;
    }

    protected static JTable table(ReadOnlyTableModel model) {
        JTable t = new JTable(model);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setToolTipText("Click a column heading to sort");
        t.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        return t;
    }

    /** Model row of the selected table row (sorting-safe), or -1 when nothing is selected. */
    protected static int selectedModelRow(JTable t) {
        int view = t.getSelectedRow();
        return view < 0 ? -1 : t.convertRowIndexToModel(view);
    }

    protected static JPanel filterBar() {
        JPanel p = new JPanel(new WrapLayout(java.awt.FlowLayout.LEFT, 8, 6));
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        return p;
    }

    protected static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AdminTheme.FONT);
        return l;
    }

    protected static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
        l.setForeground(AdminTheme.TEXT);
        return l;
    }

    /** Fills a combo box with an "all" entry followed by the items, keeping the previous selection if still present. */
    protected static void fillCombo(JComboBox<Object> box, String allText, List<?> items) {
        Object previous = box.getSelectedItem();
        box.removeAllItems();
        box.addItem(allText);
        for (Object item : items) {
            box.addItem(item);
        }
        if (previous != null) {
            box.setSelectedItem(previous);
        }
        if (box.getSelectedIndex() < 0) {
            box.setSelectedIndex(0);
        }
    }

    /** The selected item if it is a real entry (not the "all" text), otherwise null. */
    protected static <T> T selectedOf(JComboBox<Object> box, Class<T> type) {
        Object o = box.getSelectedItem();
        return type.isInstance(o) ? type.cast(o) : null;
    }

    /** Empty text means "no date"; anything else must be a valid yyyy-MM-dd date. */
    protected static LocalDate parseDate(String text, String fieldName) throws ValidationException {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException(fieldName + " must be a date written like 2026-10-25.");
        }
    }

    /** Opens a medical's supporting document in the computer's default viewer (access is checked by the service). */
    protected final void openDocument(int medicalId) {
        act("Opening the document...", () -> medicalService.getEvidencePath(medicalId), path -> {
            try {
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    showError("This computer cannot open documents from the application. File: " + path);
                    return;
                }
                Desktop.getDesktop().open(path.toFile());
                showInfo("Document opened in your default viewer.");
            } catch (IOException ex) {
                showError("The document could not be opened: " + ex.getMessage());
            }
        });
    }

    /** A caption plus its input as one unit, so the pair stays together when a filter bar wraps. */
    protected static JPanel labeled(String caption, javax.swing.JComponent input) {
        JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        JLabel l = label(caption);
        l.setLabelFor(input);
        input.getAccessibleContext().setAccessibleName(caption);
        p.add(l);
        p.add(input);
        return p;
    }

    /** Left-aligns every child of a vertical BoxLayout panel (otherwise mixed alignments shift rows sideways). */
    protected static void alignLeft(JPanel panel) {
        for (java.awt.Component c : panel.getComponents()) {
            ((javax.swing.JComponent) c).setAlignmentX(LEFT_ALIGNMENT);
        }
    }
}