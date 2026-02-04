package com.star.share.token;

import java.time.Duration;

public interface RefreshTokenRepository {

    /**
     * Save refresh token for user, used for refresh token management
     * @param userId user id
     * @param tokenId jti of refresh token, used for refresh token management, can be stored in database or cache
     * @param ttl expire duration of refresh token
     */
    void saveToken(long userId, String tokenId, Duration ttl);

    /**
     * Check if refresh token is valid for user, used for refresh token management
     * @param userId user id
     * @param tokenId jti of refresh token, used for refresh token management, can be stored in database or cache
     * @return true if token is valid, false if token is invalid or not exist
     */
    boolean isTokenValid(long userId, String tokenId);

    /**
     * Revoke refresh token for user, used for refresh token management
     * @param userId user id
     * @param tokenId jti of refresh token, used for refresh token management, can be stored in database or cache
     */
    void revokeToken(long userId, String tokenId);

    /**
     * Revoke all refresh tokens for user, used for refresh token management, e.g. when user change password or logout
     * @param userId user id
     */
    void revokeAllTokens(long userId);
}
