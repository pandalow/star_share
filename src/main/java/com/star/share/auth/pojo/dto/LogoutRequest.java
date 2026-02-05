package com.star.share.auth.pojo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Logout request
 * @param refreshToken
 */
public record LogoutRequest(
        @NotBlank(message = "refresh token cannot be blank") String refreshToken
) {
}
