package com.facultyams.service.profile.validation;

import java.util.regex.Pattern;

/** Validates a personal email and ensures it differs from the university address. */
public final class PersonalEmailValidator implements Validator<String> {
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
    private final String universityEmail;

    /** Creates the rule against the student's university email. */
    public PersonalEmailValidator(String universityEmail) {
        this.universityEmail = universityEmail == null ? "" : universityEmail;
    }

    @Override public String validate(String value) {
        if (value == null || !EMAIL.matcher(value.trim()).matches()) return "Enter a valid email address.";
        if (value.trim().equalsIgnoreCase(universityEmail)) return "Personal email must differ from university email.";
        return null;
    }
}
