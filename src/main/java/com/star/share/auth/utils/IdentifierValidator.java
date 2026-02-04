package com.star.share.auth.utils;

import java.util.regex.Pattern;

/**
 * Helper utils for verification of phone number and email
 * <p>
 * PHONE is match with ie phone number(start with 353 or 0)
 * email is like ***@*** format;
 */
public final class IdentifierValidator {
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(?:\\+353|0)(?:\\s|\\-|\\.)?(?:1|[2-9]\\d)(?:\\s|\\-|\\.)?\\d{3}(?:\\s|\\-|\\.)?\\d{4}$"
    );
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
            Pattern.CASE_INSENSITIVE
    );

    private IdentifierValidator() {
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
}
