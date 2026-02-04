package com.star.share.token;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
public class RefreshTokenRepoImpl implements RefreshTokenRepository {
    private final StringRedisTemplate redisTemplate;

    public RefreshTokenRepoImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Helper function to generate Redis key

    /**
     * Generate Redis key for refresh token
     * @param userId user id
     * @param tokenId jti of refresh token
     * @return Redis key
     */
    private static String key(long userId, String tokenId) {
        return "auth:rt:%d:%s".formatted(userId, tokenId);
    }

    /**
     * Save refresh token for user, used for refresh token management
     * @param userId user id
     * @param tokenId jti of refresh token, used for refresh token management, can be stored in database or cache
     * @param ttl expire duration of refresh token
     */
    @Override
    public void saveToken(long userId, String tokenId, Duration ttl) {
        String redisKey = key(userId, tokenId);
        redisTemplate.opsForValue().set(redisKey, "1", ttl);
    }
    /**
     * Check if refresh token is valid for user, used for refresh token management
     * @param userId user id
     * @param tokenId jti of refresh token, used for refresh token management, can be stored in database or cache
     * @return true if token is valid, false if token is invalid or not exist
     */
    @Override
    public boolean isTokenValid(long userId, String tokenId) {
        String key = key(userId, tokenId);
        return Objects.equals(redisTemplate.opsForValue().get(key), "1");
    }

    /**
     * Revoke refresh token for user, used for refresh token management
     * @param userId user id
     * @param tokenId jti of refresh token, used for refresh token management, can be stored in database or cache
     */
    @Override
    public void revokeToken(long userId, String tokenId) {
        String key = key(userId, tokenId);
        redisTemplate.delete(key);
    }

    /**
     * Revoke all refresh tokens for user, used for refresh token management, e.g. when user change password or logout
     * @param userId user id
     */
    @Override
    public void revokeAllTokens(long userId) {
        String pattern = "auth:rt:%d:*".formatted(userId);
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
