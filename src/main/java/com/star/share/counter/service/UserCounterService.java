package com.star.share.counter.service;

public interface UserCounterService {
    /** Increment the number of followings for a user by a specified delta. */
    void incrementFollowings(long userId, int delta);
    /** Increment the number of followers for a user by a specified delta. */
    void incrementFollowers(long userId, int delta);
    /** Increment the number of favorites for a user by a specified delta. */
    void incrementPosts(long userId, int delta);
    /** Increment the number of likes (Authors) received for a user by a specified delta. */
    void incrementLikesReceived(long userId, int delta);
    /** Increment the number of favorites(Authors) received for a user by a specified delta. */
    void incrementFavsReceived(long userId, int delta);
    /** Rebuild all counters for a user by recalculating them from the database. */
    void rebuildCounters(long userId);
}
