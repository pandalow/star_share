package com.star.share.auth.pojo.dto;

import com.star.share.auth.enumerate.IdentifierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Password reset request
 * @param identifierType
 * @param identifier
 * @param code
 * @param newPassword
 */
public record PasswordResetRequest(
        @NotNull(message="Identifier type cannot be null") IdentifierType identifierType,
        @NotBlank(message="Identifier cannot be blank") String identifier,
        @NotBlank(message="Verification code cannot be blank") String code,
        @NotBlank(message="New password cannot be blank") String newPassword
) {
}
