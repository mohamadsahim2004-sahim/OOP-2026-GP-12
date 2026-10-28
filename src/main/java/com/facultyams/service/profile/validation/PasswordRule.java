package com.facultyams.service.profile.validation;

/** Strategy for evaluating one password requirement without converting secrets to Strings. */
public interface PasswordRule {
    /** Returns the user-facing rule label. */
    String label();

    /** Evaluates a candidate password against the current password. */
    boolean isSatisfied(char[] candidate, char[] current);
}
