package com.star.share.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    IDENTIFIER_EXISTS("IDENTIFIER_EXISTS", "identifier exists"),
    IDENTIFIER_NOT_FOUND("IDENTIFIER_NOT_FOUND", "identifier not found"),
    ZGID_EXISTS("ZGID_EXISTS", "zgid exists"),
    VERIFICATION_RATE_LIMIT("VERIFICATION_RATE_LIMIT", "verification code send rate limit exceeded"),
    VERIFICATION_DAILY_LIMIT("VERIFICATION_DAILY_LIMIT", "verification code daily send limit exceeded"),
    VERIFICATION_NOT_FOUND("VERIFICATION_NOT_FOUND", "verification code not found"),
    VERIFICATION_MISMATCH("VERIFICATION_MISMATCH", "verification code mismatch"),
    VERIFICATION_TOO_MANY_ATTEMPTS("VERIFICATION_TOO_MANY_ATTEMPTS", "too many verification code attempts"),
    PASSWORD_POLICY_VIOLATION("PASSWORD_POLICY_VIOLATION", "password policy violation"),
    BAD_REQUEST("BAD_REQUEST", "bad request"),
    TERMS_NOT_ACCEPTED("TERMS_NOT_ACCEPTED", "terms of service not accepted"),
    REFRESH_TOKEN_INVALID("REFRESH_TOKEN_INVALID", "refresh token invalid"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "internal server error"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "invalid credentials");



    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
