package com.star.share.posts.entity.dto;

import jakarta.validation.constraints.NotBlank;

public record DescriptionSuggestRequest(
        @NotBlank(message = "content could not be blank") String content
) {}