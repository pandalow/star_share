package com.star.share.posts.entity.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    private Long id;
    private Long tagId;
    /** Json tag list, example: ["tag1","tag2"] */
    private String tags;
    private String title;
    private String description;
    private String contentUrl;
    private String contentObjectKey;
    private String contentEtag;
    private Long contentSize;
    private String contentSha256; //
    private Long creatorId;
    private Boolean isTop;
    private String type;
    private String visible;
    /** Json image URL list, example: ["http://example.com/image1.jpg","http://example.com/image2.jpg"] */
    private String imgUrls;
    private String videoUrl;
    private String status;
    private Instant createTime;
    private Instant updateTime;
    private Instant publishTime;

}
