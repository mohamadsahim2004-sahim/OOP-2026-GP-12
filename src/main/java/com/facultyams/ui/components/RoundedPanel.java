package com.facultyams.ui.components;

import com.facultyams.ui.theme.Theme;
import java.awt.Color;

/** White rounded surface with the shared timetable card treatment. */
public class RoundedPanel extends CardPanel {
    public RoundedPanel() { super(Theme.CARD_RADIUS); }
    public RoundedPanel(int radius) { super(radius); }
    public RoundedPanel(int radius, Color background) { super(radius, background); }
}
