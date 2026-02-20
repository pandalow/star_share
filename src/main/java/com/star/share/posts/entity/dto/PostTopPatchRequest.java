package com.star.share.posts.entity.dto;

import jakarta.validation.constraints.NotNull;

public record PostTopPatchRequest(
        @NotNull Boolean isTop
) {}
