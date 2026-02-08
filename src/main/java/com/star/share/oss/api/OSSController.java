package com.star.share.oss.api;

import com.star.share.auth.token.JwtService;
import com.star.share.common.exception.BusinessException;
import com.star.share.common.exception.ErrorCode;
import com.star.share.oss.entity.OSSPresignRequest;
import com.star.share.oss.entity.OSSPresignResponse;
import com.star.share.oss.service.OSSService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * OSSController provides REST API endpoints for generating pre-signed URLs
 * for uploading content or images related to "know posts".
 */
@RestController
@RequestMapping("/api/v1/storage")
@Validated
@RequiredArgsConstructor
public class OSSController {
    private final OSSService ossService;
    private final JwtService jwtService;
    // TODO: add know post mapper for post existence and ownership check
    //    private final KnowPostMapper knowPostMapper;

    /**
     * Generate pre-signed URL for uploading content or image of know post
     */
    @PostMapping("/presign")
    public OSSPresignResponse presign(@Valid @RequestBody OSSPresignRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);

        long postId;
        try {
            postId = Long.parseLong(request.postId());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Illegal postId format");
        }

        // Validate post existence and ownership
        // TODO: add know post mapper for post existence and ownership check

        String scene = request.scene();
        String objectKey;
        String ext = normalizeExt(request.ext(), request.contentType(), scene);

        if ("knowpost_content".equals(scene)) {
            objectKey = "posts/" + postId + "/content" + ext;
        } else if ("knowpost_image".equals(scene)) {
            String date = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC")).format(Instant.now());
            String rand = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
            objectKey = "posts/" + postId + "/images/" + date + "/" + rand + ext;
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "unsupported scene");
        }

        int expiresIn = 600; // 10 分钟
        String putUrl = ossService.generatePresignedPutUrl(objectKey, request.contentType(), expiresIn);
        Map<String, String> headers = Map.of("Content-Type", request.contentType());
        return new OSSPresignResponse(objectKey, putUrl, headers, expiresIn);
    }

    private String normalizeExt(String ext, String contentType, String scene) {
        if (ext != null && !ext.isBlank()) {
            return ext.startsWith(".") ? ext : "." + ext;
        }
        if ("knowpost_content".equals(scene)) {
            return switch (contentType) {
                case "text/markdown" -> ".md";
                case "text/html" -> ".html";
                case "text/plain" -> ".txt";
                case "application/json" -> ".json";
                default -> ".bin";
            };
        } else {
            return switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".img";
            };
        }
    }
}
