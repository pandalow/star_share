package com.star.share.oss.entity;

import java.util.Map;

/**
 * Presign response for client to upload file to OSS directly
 * @param objectKey
 * @param putUrl
 * @param headers
 * @param expireIn
 */
public record OSSPresignResponse(String objectKey,
                                 String putUrl,
                                 Map<String, String> headers,
                                 int expireIn) {
}
