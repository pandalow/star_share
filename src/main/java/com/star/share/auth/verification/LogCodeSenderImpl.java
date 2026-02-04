package com.star.share.auth.verification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Log code sender implementation
 *
 * Not to send actual code, just log it, easy for development and testing
 */
@Slf4j
@Component
public class LogCodeSenderImpl implements CodeSender {

    /**
     * Record send code to identifier
     * @param scene scene name
     * @param identifier identifier(phone or email)
     * @param code code to send
     * @param expireMinutes code expire time in minutes
     */
    @Override
    public void sendCode(String scene, String identifier, String code, int expireMinutes) {
        log.info("Sending code [{}] for scene [{}] to identifier [{}], expire in {} minutes",
                code, scene, identifier, expireMinutes);
    }
}
