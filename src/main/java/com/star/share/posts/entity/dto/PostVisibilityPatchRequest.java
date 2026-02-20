package com.star.share.posts.entity.dto;

import jakarta.validation.constraints.NotBlank;

public record PostVisibilityPatchRequest(
        @NotBlank String visible
) {}