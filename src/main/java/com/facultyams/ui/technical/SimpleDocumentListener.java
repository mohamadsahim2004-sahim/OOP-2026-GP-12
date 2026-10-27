package com.facultyams.ui.technical;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** A DocumentListener that reacts the same way to every kind of change (lets lambdas be used). */
@FunctionalInterface
interface SimpleDocumentListener extends DocumentListener {

    void changed();

    @Override
    default void insertUpdate(DocumentEvent e) {
        changed();
    }

    @Override
    default void removeUpdate(DocumentEvent e) {
        changed();
    }

    @Override
    default void changedUpdate(DocumentEvent e) {
        changed();
    }
}