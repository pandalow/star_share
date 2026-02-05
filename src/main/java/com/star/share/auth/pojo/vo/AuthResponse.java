package com.star.share.auth.pojo.vo;

/**
 * Authentication response, including user info and token info
 * @param user
 * @param token
 */
public record AuthResponse(
        AuthUserResponse user,
        TokenResponse token
) {
}
