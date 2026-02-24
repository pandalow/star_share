package com.star.share.posts.service;

public interface FeedCacheService {
    
    /**
     * Delete all feed page caches (public and personal) to ensure consistency after
     * content changes that affect feed visibility or ordering.
     * 
     * - keys pattern: "feed:public:*" for public feed pages
     */
    void deleteAllCache();

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
    void doubleDeleteCache(long delayMillis);

    /**
     * Delete personal feed caches for a specific user, used when their content
     * changes affect their feed visibility.
     * - keys pattern: "feed:mine:{userId}:*" for personal feed pages of the user
     */
    void deleteMyFeedCache(long userId);

    /**
     * Double delete strategy for personal feed caches of a specific user.
     *
     * @param userId the ID of the user whose personal feed caches should be deleted
     * @param delayMillis the delay in milliseconds before performing the second
     *                    deletion
     */
    void doubleDeleteMyFeedCache(long userId, long delayMillis);
}
