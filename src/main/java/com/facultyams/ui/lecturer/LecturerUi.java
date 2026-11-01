package com.facultyams.ui.lecturer;

import com.facultyams.ui.admin.AdminTheme;
import com.facultyams.util.UIUtil;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.Cursor;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Small helpers shared by the Lecturer screens. */
public final class LecturerUi {

    /** Checks run without a screen set this so work finishes before the next statement. */
    static volatile boolean synchronous;

    private LecturerUi() {
    }

    /**
     * Runs {@code job} off the Swing thread (status shows "Loading..."), then hands the result to
     * {@code onSuccess} on the Swing thread. Any failure is shown to the user (dialog + status
     * line) and never leaves the screen stuck in the loading state.
     */
    public static <T> void load(Component owner, JLabel status, Callable<T> job, Consumer<T> onSuccess) {
        status.setText("Loading...");
        if (owner != null) {
            owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        if (synchronous) {
            try {
                T value = job.call();
                finish(owner, null);
                onSuccess.accept(value);
            } catch (Exception e) {
                fail(owner, status, e);
            }
            return;
        }
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return job.call();
            }

            @Override
            protected void done() {
                try {
                    T value = get();
                    finish(owner, null);
                    onSuccess.accept(value);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    fail(owner, status, cause instanceof Exception ex ? ex : e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail(owner, status, e);
                }
            }
        }.execute();
    }

    private static void finish(Component owner, Object ignored) {
        if (owner != null) {
            owner.setCursor(Cursor.getDefaultCursor());
        }
    }

    private static void fail(Component owner, JLabel status, Exception e) {
        finish(owner, null);
        status.setText("Could not complete the request: " + e.getMessage());
        UIUtil.showException(owner, e);
    }

    /** A table model whose cells cannot be edited. */
    static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    static JTable styledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        AdminTheme.styleTable(table);
        table.setAutoCreateRowSorter(true);
        return table;
    }

    /**
     * Lets a search box (matches registration number and name, columns 0 and 1) and an optional batch combo
     * box ("All batches" or a batch; matched against {@code batchColumn}) filter a sortable table.
     */
    static void installFilter(JTable table, javax.swing.JTextField search, javax.swing.JComboBox<String> batch,
                              int batchColumn) {
        Runnable apply = () -> {
            String text = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            String selectedBatch = batch == null || batch.getSelectedIndex() <= 0 ? null
                    : String.valueOf(batch.getSelectedItem());
            @SuppressWarnings("unchecked")
            javax.swing.table.TableRowSorter<javax.swing.table.TableModel> sorter =
                    (javax.swing.table.TableRowSorter<javax.swing.table.TableModel>) table.getRowSorter();
            sorter.setRowFilter(new javax.swing.RowFilter<javax.swing.table.TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends javax.swing.table.TableModel, ? extends Integer> e) {
                    boolean textOk = text.isEmpty()
                            || String.valueOf(e.getValue(0)).toLowerCase().contains(text)
                            || String.valueOf(e.getValue(1)).toLowerCase().contains(text);
                    boolean batchOk = selectedBatch == null || selectedBatch.equals(String.valueOf(e.getValue(batchColumn)));
                    return textOk && batchOk;
                }
            });
        };
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { apply.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { apply.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { apply.run(); }
        });
        if (batch != null) {
            batch.addActionListener(e -> apply.run());
        }
    }

    /** Fills a batch combo box with "All batches" plus the distinct batches (keeps the selection if possible). */
    static void fillBatches(javax.swing.JComboBox<String> combo, java.util.Collection<String> batches) {
        Object keep = combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem("All batches");
        new java.util.TreeSet<>(batches).forEach(combo::addItem);
        if (keep != null) {
            combo.setSelectedItem(keep);
        }
    }

    /** Two-decimal text for a percentage / mark; an en dash when there is no value. */
    static String num(Double value) {
        return value == null ? "-" : String.format("%.2f", value);
    }

    static String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }
}
