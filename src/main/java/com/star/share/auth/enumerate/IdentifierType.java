package com.star.share.auth.enumerate;

public enum IdentifierType {
    PHONE, EMAIL;

    public static IdentifierType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Identifier type required");
        }

        return switch (value.toLowerCase()) {
            case "mobile", "phone" -> PHONE;
            case "email", "e-mail" -> EMAIL;
            default -> throw new IllegalArgumentException("Unsupported Identifier type: " + value);
        };
    }
}

