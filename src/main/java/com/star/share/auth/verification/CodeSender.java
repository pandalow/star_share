package com.star.share.auth.verification;

/**
 * Code sender interface
 */

public interface CodeSender {
    /**
     * Send code to identifier
     * @param scene scene name
     * @param identifier identifier(phone or email)
     * @param code code to send
     * @param expireMinutes code expire time in minutes
     */
    void sendCode(String scene, String identifier, String code, int expireMinutes);
}
