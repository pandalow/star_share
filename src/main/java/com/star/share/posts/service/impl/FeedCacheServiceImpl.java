package com.star.share.posts.service.impl;

import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.star.share.posts.service.FeedCacheService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedCacheServiceImpl implements FeedCacheService {
    private final StringRedisTemplate redis;

    /**
     * Delete all feed page caches (public and personal) to ensure consistency after
     * content changes that affect feed visibility or ordering.
     * 
     * - keys pattern: "feed:public:*" for public feed pages
     */
    @Override
    public void deleteAllCache() {
        Set<String> keys = redis.keys("feed:public:*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    /**
     * Double delete strategy to mitigate cache consistency issues in distributed
     * environments.
     * Deletes all feed caches immediately, then again after a short delay to catch
     * any stragglers.
     *
     * - keys pattern: "feed:public:*" for public feed pages
     * 
     * @param delayMillis the delay in milliseconds before performing the second
     *                    deletion
     */
    @Override
    public void doubleDeleteCache(long delayMillis) {
        deleteAllCache();
        try {
            Thread.sleep(Math.max(delayMillis, 50));
        } catch (InterruptedException e) {
        }
        deleteAllCache();
    }

    /**
     * Delete personal feed caches for a specific user, used when their content
     * changes affect their feed visibility.
     * - keys pattern: "feed:mine:{userId}:*" for personal feed pages of the user
     */
    @Override
    public void deleteMyFeedCache(long userId) {
        Set<String> keys = redis.keys("feed:mine:" + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    /**
     * Double delete strategy for personal feed caches of a specific user.
     * Deletes the user's personal feed caches immediately, then again after a short
     * delay.
     *
     * - keys pattern: "feed:mine:{userId}:*" for personal feed pages of the user
     * 
     * @param userId      the ID of the user whose personal feed caches should be
     *                    deleted
     * @param delayMillis the delay in milliseconds before performing the second
     *                    deletion
     */
    @Override
    public void doubleDeleteMyFeedCache(long userId, long delayMillis) {
        deleteMyFeedCache(userId);
        try {
            Thread.sleep(Math.max(delayMillis, 50));
        } catch (InterruptedException e) {
        }
        deleteMyFeedCache(userId);
    }

}
