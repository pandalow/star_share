package com.star.share.auth.config;

import lombok.Data;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.time.Duration;

/**
 * Identification properties
 * - Jwt
 * - verification:
 * - Password:
 */
@Data
@Configuration
public class AuthProperties {

    private final Jwt jwt = new Jwt();
    private final Verification verification = new Verification();
    private final Password password = new Password();
    @Data
    public static class Jwt{
        /** Jwt Issuer */
        private String issuer = "sharestar";
        /** Expire duration */
        private Duration accessTokent1 = Duration.ofMinutes(15);
        /** Refresh token duration */
        private Duration refreshTokent1 = Duration.ofDays(7);
        /** JWK keys: wrap RSA keys*/
        private String keyId = "sharestar-key";
        /** RSA private PEM */
        private Resource privateKey;
        /** RSA public PEM */
        private Resource publicKey;

    }

    @Data
    public static class Verification{
        /** verification length */
        private int codeLength = 6;
        /** verification duration */
        private Duration ttl = Duration.ofMinutes(5);
        /** verification attempts */
        private int maxAttempts = 5;
        /** Intervals of min continuously sent */
        private Duration sendInterval = Duration.ofSeconds(60);
        /** Daily limitation */
        private int dailyLimit = 10;
    }

    /** password config */
    @Data
    public static class Password{
        /** BCrypt cost */
        private int bcryptStrength = 12;
        /** password minimum length */
        private int minLength = 8;
    }

}
