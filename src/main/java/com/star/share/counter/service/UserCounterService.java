package com.star.share.counter.service;

public interface UserCounterService {
    /* Increment the number of followings for a user by a specified delta */
    void incrementFollowings(long userId, int delta);
    /* Increment the number of followers for a user by a specified delta */
    void incrementFollowers(long userId, int delta);
    /* Increment the number of posts for a user by a specified delta */
    void incrementPosts(long userId, int delta);
    /* Increment the number of likes received by a user by a specified delta */
    void incrementLikesReceived(long userId, int delta);
    /* Increment the number of favorites received by a user by a specified delta */
    void incrementFavoritesReceived(long userId, int delta);
    /* Rebuild all counters for a user */
    void rebuildAllCounters(long userId);
}
