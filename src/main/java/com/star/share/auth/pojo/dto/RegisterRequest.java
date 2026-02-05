package com.star.share.auth.pojo.dto;

import com.star.share.auth.enumerate.IdentifierType;
import jakarta.validation.constraints.NotBlank;

import javax.validation.constraints.NotNull;

/**
 * Register request
 * @param identifierType
 * @param identifier
 * @param code
 * @param password
 * @param agreeTerms
 */
public record RegisterRequest(
        @NotNull(message = "Identifier type cannot be null") IdentifierType identifierType,
        @NotBlank(message = "Identifier cannot be blank") String identifier,
        @NotBlank(message = "Verification code cannot be blank") String code,
        String password,
        boolean agreeTerms
) {
}
