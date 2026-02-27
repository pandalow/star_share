package com.star.share.posts.service;

import com.star.share.posts.entity.vo.FeedPageResponse;

public interface PostFeedService {

    FeedPageResponse getFeed(int page, int size, Long currentUserIdNullable);


    FeedPageResponse getMyFeed(long userId, int page, int size);
}
