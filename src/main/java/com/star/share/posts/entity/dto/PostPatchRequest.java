package com.star.share.posts.entity.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Update the post. All fields are optional, only the provided fields will be updated.
 */
public record PostPatchRequest(
        String title,
        Long tagId,
        @Size(max = 20) List<String> tags,
        @Size(max = 20) List<String> imgUrls,
        String visible,
        String isTop,
        String description
) {
}
