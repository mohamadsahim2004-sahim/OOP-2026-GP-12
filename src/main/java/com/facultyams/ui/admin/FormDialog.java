package com.facultyams.ui.admin;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.function.BooleanSupplier;

/** Shows an Add/Edit form in a modal dialog with Save and Cancel buttons (shared by the management screens). */
final class FormDialog {

    private FormDialog() {
    }

    /**
     * @param save called when Save is pressed; returns true when the data was saved (the dialog then closes)
     *             and false when an error was shown (the dialog stays open)
     * @return true if the data was saved
     */
    static boolean show(Component parent, String title, JPanel form, String saveText, BooleanSupplier save) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, JDialog.DEFAULT_MODALITY_TYPE);
        boolean[] saved = {false};
        JButton saveButton = new JButton(saveText);
        JButton cancelButton = new JButton("Cancel");
        saveButton.addActionListener(e -> {
            if (save.getAsBoolean()) {
                saved[0] = true;
                dialog.dispose();
            }
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(cancelButton);
        buttons.add(saveButton);
        JPanel content = new JPanel(new BorderLayout());
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        AdminTheme.apply(content);
        AdminTheme.secondary(cancelButton);
        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton(saveButton);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return saved[0];
    }
}