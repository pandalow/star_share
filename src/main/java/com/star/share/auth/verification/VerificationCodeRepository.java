package com.star.share.auth.verification;

import com.star.share.auth.pojo.VerificationCheckResult;

import java.time.Duration;

/**
 * Abstract interface for verification code
 * Allow for Redis to implementation
 */
public interface VerificationCodeRepository {
    /**
     * Save verification code
     * @param scene scene name
     * @param identifier phone number or email address
     * @param code verification code
     * @param ttl duration
     * @param maxAttempts
     */
    void saveCode(String scene, String identifier, String code, Duration ttl, int maxAttempts);

    /**
     * Verify code
     * @param scene scene name
     * @param identifier phone number or email address
     * @param code verification code
     * @return Verification Result contains: status, attempts
     */
    VerificationCheckResult verify(String scene, String identifier, String code);

    /**
     * make verification code invalid
     * @param scene scene name
     * @param identifier phone number or email address
     */
    void invalidate(String scene, String identifier);
}
