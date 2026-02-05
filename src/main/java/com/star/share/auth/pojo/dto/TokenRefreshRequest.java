package com.star.share.auth.pojo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Token refresh request
 * @param refreshToken
 */
public record TokenRefreshRequest(
        @NotBlank(message = "refresh token cannot be blank") String refreshToken
) {
}
