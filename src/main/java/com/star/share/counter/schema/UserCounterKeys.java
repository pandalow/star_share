package com.star.share.counter.schema;

/**
 * Redis key definitions for user-specific counters,
 * such as total likes and favorites for a user.
 */
public final class UserCounterKeys {
    private UserCounterKeys() {
        // Prevent instantiation
    }

    public static String sdsKey(long userId){
        return "ucnt:" + userId;
    }
}
