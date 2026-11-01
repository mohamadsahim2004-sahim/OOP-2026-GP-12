package com.facultyams.ui.admin;

/** Implemented by every Admin screen that reloads its data from the database when it is shown. */
public interface Refreshable {

    /** Reloads the data shown on the screen. Must report failures to the user itself. */
    void refresh();
}