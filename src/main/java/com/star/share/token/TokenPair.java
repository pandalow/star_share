package com.star.share.token;

import java.time.Instant;

/**
 * Token pair of access token and refresh token
 * @param accessToken access token string(Jwt string, Bearer token string)
 * @param accessTokenExpireAt   access token expire time
 * @param refreshToken refresh token string(Jwt string, Bearer token string)
 * @param refreshTokenExpireAt refresh token expire time
 * @param refreshTokenId    refresh token id, used for refresh token management, can be stored in database or cache
 */
public record TokenPair(String accessToken,
                        Instant accessTokenExpireAt, String refreshToken,
                        Instant refreshTokenExpireAt, String refreshTokenId) {
}
