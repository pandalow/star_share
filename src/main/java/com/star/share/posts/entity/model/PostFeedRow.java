package com.star.share.posts.entity.model;

import lombok.Data;

import java.time.Instant;

@Data
public class PostFeedRow {
    private Long id;
    private String title;
    private String description;
    private String tags; // Json string
    private String imgUrls; //  Json string
    private String authorAvatar;
    private String authorNickname;
    private String authorTagJson; //  Author tags in domain-specific format, e.g. ["tag1","tag2"]
    private Instant publishTime;
    private Boolean isTop;

}
