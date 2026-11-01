package com.facultyams.ui.admin;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.function.BooleanSupplier;

/**
 * Shows an Add/Edit form in a modal dialog with a heading, Save and Cancel buttons (shared by the management
 * screens). Enter saves and Escape cancels, so forms can be completed from the keyboard.
 */
final class FormDialog {

    private static final int MIN_WIDTH = 460;

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

        JLabel heading = new JLabel(title);
        heading.setFont(AdminTheme.SECTION);
        heading.setForeground(AdminTheme.TEXT);
        heading.setBorder(BorderFactory.createEmptyBorder(20, 22, 4, 22));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AdminTheme.BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        buttons.add(cancelButton);
        buttons.add(saveButton);

        JPanel content = new JPanel(new BorderLayout());
        content.add(heading, BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        AdminTheme.apply(content);
        AdminTheme.secondary(cancelButton);
        content.setOpaque(true);
        content.setBackground(AdminTheme.PAGE_BG);
        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton(saveButton);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(MIN_WIDTH, dialog.getHeight()));
        dialog.setSize(Math.max(MIN_WIDTH, dialog.getWidth()), dialog.getHeight());
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return saved[0];
    }
}