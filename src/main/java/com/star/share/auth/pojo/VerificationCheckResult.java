package com.star.share.auth.pojo;

import com.star.share.auth.enumerate.VerificationCodeStatus;

/**
 * Verification result
 * Contains:
     * SUCCESS,
     * NOT_FOUND,
     * EXPIRED,
     * MISMATCH,
     * TOO_MANY_ATTEMPTS
 *
 * @param status
 * @param attempts
 * @param maxAttempts
 */
public record VerificationCheckResult(VerificationCodeStatus status, int attempts, int maxAttempts) {
    public boolean isSuccess(){
        return status == VerificationCodeStatus.SUCCESS;
    }
}
