package com.star.share.auth.pojo.dto;


import com.star.share.auth.enumerate.IdentifierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Login request
 * @param identifierType
 * @param identifier
 * @param code
 * @param password
 */
public record LoginRequest (
        @NotNull(message = "Identifier type cannot be null") IdentifierType identifierType,
        @NotBlank(message = "Identifier cannot be blank") String identifier,
        String code,
        String password
){
}
