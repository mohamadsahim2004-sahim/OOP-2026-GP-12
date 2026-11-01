package com.facultyams.service.profile.validation;

/** Validates common Sri Lankan mobile number formats. */
public final class SriLankanPhoneValidator implements Validator<String> {
    @Override public String validate(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        boolean valid = digits.length() == 10 && digits.matches("07\\d{8}")
                || digits.length() == 11 && digits.matches("947\\d{8}");
        return valid ? null : "Use 07X XXX XXXX or +94 7X XXX XXXX.";
    }
}
