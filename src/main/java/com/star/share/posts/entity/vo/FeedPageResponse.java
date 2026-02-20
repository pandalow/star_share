package com.star.share.posts.entity.vo;

import java.util.List;

/**
 * The Response of the feed page, which contains a list of feed items and pagination information.
 * @param items
 * @param page
 * @param size
 * @param hasMore
 */
public record FeedPageResponse(
        List<FeedItemResponse> items,
        int page,
        int size,
        boolean hasMore
) {}