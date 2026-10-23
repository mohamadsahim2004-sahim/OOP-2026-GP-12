package com.facultyams.model.profile;

import com.facultyams.util.Constants;
import java.util.Locale;

/** Academic classification thresholds for the 4.0 GPA scale. */
public final class GpaClassifier {
    /** Returns the classification for a GPA in the 0.0 to 4.0 range. */
    public String classify(double gpa) {
        if (!Double.isFinite(gpa) || gpa < 0.0 || gpa > Constants.MAX_GPA) {
            throw new IllegalArgumentException("GPA must be between 0.00 and "
                    + String.format(Locale.ROOT, "%.2f", Constants.MAX_GPA) + ".");
        }
        if (gpa >= 3.70) return "First Class";
        if (gpa >= 3.30) return "Second Upper";
        if (gpa >= 3.00) return "Second Lower";
        if (gpa >= 2.00) return "General";
        return "Probation";
    }
}
