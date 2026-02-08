package com.star.share.oss.entity;

import jakarta.validation.constraints.NotBlank;

/**
 * Request for generating OSS pre-signed URL
 *
 * @param scene
 * @param postId
 * @param contentType
 * @param ext
 */
public record OSSPresignRequest(
        @NotBlank String scene, // knowpost_content | knowpost_image
        @NotBlank String postId,
        @NotBlank String contentType,
        String ext
) {
}
