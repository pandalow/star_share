package com.star.share.oss.service;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.PutObjectRequest;
import com.star.share.common.exception.BusinessException;
import com.star.share.common.exception.ErrorCode;
import com.star.share.oss.config.OSSProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;

/**
 * OSSService provides methods to interact with Object Storage Service (OSS), such as uploading files and generating presigned URLs.
 * It uses Alibaba Cloud OSS SDK to perform operations on OSS. The service requires configuration of OSS properties
 * such as endpoint, access key, secret key, bucket name, and optional public domain.
 */
@Service
@RequiredArgsConstructor
public class OSSService {
    private final OSSProperties props;

    /**
     * Upload avatar file for user and return public URL of the uploaded file.
     * The file will be stored in OSS with a key based on user ID and current timestamp to avoid conflicts.
     * @param userId user ID for which the avatar is being uploaded
     * @param file multipart file containing the avatar image
     * @return public URL of the uploaded avatar image
     * @throws BusinessException if there is an error during file upload or if OSS is not properly configured
     */
    public String uploadAvatar(long userId, MultipartFile file) {
        ensureConfigured();

        String originalFilename = file.getOriginalFilename();
        String ext = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectKey = props.getFolder() + "/" + userId + "/avatar" + Instant.now().toEpochMilli() + ext;

        OSS client = new OSSClientBuilder().build(
                props.getEndpoint(),
                props.getAccessKeyId(),
                props.getAccessKeySecret()
        );
        try {
            PutObjectRequest request = new PutObjectRequest(
                    props.getBucket(),
                    objectKey,
                    file.getInputStream()
            );
            client.putObject(request);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Failed to upload file");
        } finally {
            client.shutdown();
        }
        return publicUrl(objectKey);
    }

    /**
     * Generate a presigned URL for uploading a file to OSS.
     * The URL will allow clients to upload a file directly to OSS without going through the server.
     * @param objectKey the key (path) in OSS where the file will be uploaded, e.g. "folder/userId/file.jpg"
     * @param contentType the content type of the file to be uploaded, e.g. "image/jpeg". This is optional but can help OSS to handle the file correctly.
     * @param expireInSeconds   the expiration time of the presigned URL in seconds. After this time, the URL will no longer be valid.
     * @return the generated presigned URL as a string
     */
    public String generatePresignedPutUrl(String objectKey, String contentType, int expireInSeconds) {
        ensureConfigured();
        OSS client = new OSSClientBuilder().build(props.getEndpoint(), props.getAccessKeyId(), props.getAccessKeySecret());
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireInSeconds * 1000L);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(props.getBucket(), objectKey, HttpMethod.PUT);
            request.setExpiration(expiration);
            if (contentType != null && !contentType.isBlank()) {
                request.setContentType(contentType);
            }
            URL url = client.generatePresignedUrl(request);
            return url.toString();
        } finally {
            client.shutdown();
        }
    }

    // Helper function
    private String publicUrl(String objectKey) {
        if (props.getPublicDomain() != null && !props.getPublicDomain().isBlank()) {
            return props.getPublicDomain().replaceAll("/$", "") + "/" + objectKey;
        }
        return "https://" + props.getBucket() + "." + props.getEndpoint() + "/" + objectKey;
    }

    private void ensureConfigured() {
        if (props.getEndpoint() == null || props.getAccessKeyId() == null || props.getAccessKeySecret() == null || props.getBucket() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "对象存储未配置");
        }
    }
}
