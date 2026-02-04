package com.star.share.auth.enumerate;

public enum VerificationCodeStatus {
    SUCCESS,
    NOT_FOUND,
    EXPIRED,
    MISMATCH,
    TOO_MANY_ATTEMPTS
}
