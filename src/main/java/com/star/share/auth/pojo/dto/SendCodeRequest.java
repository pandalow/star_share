package com.star.share.auth.pojo.dto;

import com.star.share.auth.enumerate.IdentifierType;
import com.star.share.auth.enumerate.VerificationScene;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Send code request
 * @param scene
 * @param identifierType
 * @param identifier
 */
public record SendCodeRequest(
        @NotNull(message="Scene cannot be null")VerificationScene scene,
        @NotNull(message="Identifier Type cannot be null") IdentifierType identifierType,
        @NotBlank(message="Identifier cannot be blank") String identifier
        ) {
}
