package com.star.share.posts.entity.vo;

import java.time.Instant;
import java.util.List;

/**
 * Post detail response for KnowPost, used in the post detail API response.
 */
public record PostDetailResponse(
        String id,
        String title,
        String description,
        String contentUrl,
        List<String> images,
        List<String> tags,
        String authorId,
        String authorAvatar,
        String authorNickname,
        String authorTagJson,
        Long likeCount,
        Long favoriteCount,
        Boolean liked,
        Boolean faved,
        Boolean isTop,
        String visible,
        String type,
        Instant publishTime
) {}