package com.facultyams.service.profile.validation;

/** Requires a residential address between 10 and 150 characters. */
public final class ResidentialAddressValidator implements Validator<String> {
    @Override public String validate(String value) {
        int length = value == null ? 0 : value.trim().length();
        return length >= 10 && length <= 150 ? null : "Address must be 10 to 150 characters.";
    }
}
