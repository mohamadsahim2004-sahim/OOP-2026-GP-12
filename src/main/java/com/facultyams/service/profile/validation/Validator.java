package com.facultyams.service.profile.validation;

/** Strategy contract for validating a profile input value. */
@FunctionalInterface
public interface Validator<T> {
    /** Returns a user-facing validation error, or null when the value is valid. */
    String validate(T value);
}
