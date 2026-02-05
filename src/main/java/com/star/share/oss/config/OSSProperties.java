package com.star.share.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OSSProperties {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucket;
    private String publicDomain; // Optional public domain for accessing files, if not set, will use bucket domain
    private String folder = "avatars"; // Optional folder for storing files, default to "avatars"
}
