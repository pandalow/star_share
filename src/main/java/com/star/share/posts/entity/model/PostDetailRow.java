package com.star.share.posts.entity.model;

import lombok.Data;

import java.time.Instant;

@Data
public class PostDetailRow {
    private Long id;
    private Long creatorId;
    private String title;
    private String description;
    private String tags;
    private String imgUrls;
    private String contentUrl;
    private String contentEtag;
    private String contentSha256;
    private String authorAvatar;
    private String authorNickname;
    private String authorTagJson;
    private Instant publishTime;
    private Boolean isTop;
    private String visible;
    private String type;
    private String status;
}
