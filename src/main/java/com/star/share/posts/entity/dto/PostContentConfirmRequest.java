package com.star.share.posts.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostContentConfirmRequest(
        @NotBlank String objectKey,
        @NotBlank String etag,
        @NotNull Long size,
        @NotBlank String sha256
) {}