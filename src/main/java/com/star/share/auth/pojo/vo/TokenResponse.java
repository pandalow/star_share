package com.star.share.auth.pojo.vo;

import java.time.Instant;

/**
 * Token response
 * @param accessToken
 * @param accessTokenExpireAt
 * @param refreshToken
 * @param refreshTokenExpireAt
 */
public record TokenResponse(
        String accessToken,
        Instant accessTokenExpireAt,
        String refreshToken,
        Instant refreshTokenExpireAt
) {
}
