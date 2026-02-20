package com.star.share.counter.schema;

/**
 * Utility class for bitmap sharding logic.
 * Using static methods to calculate chunk and bit positions for user IDs.
 */
public final class BitmapShard {

    public static final int CHUNK_SIZE = 32_768;
    public static long chunkOf(long userId){
        return userId/CHUNK_SIZE;
    }
    public static long bitOf(long userId){
        return userId%CHUNK_SIZE;
    }
    private BitmapShard() {
        // Prevent instantiation
    }
}
