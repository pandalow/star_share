package com.star.share.counter.schema;

/**
 * Redis key definitions for counters,
 * including patterns and prefixes.
 */
public final class CounterKeys {
    private CounterKeys() {
        // Prevent instantiation
    }

    public static String sdsKey(String entityType, String entityId) {
        // Key SDS format: cnt:v1:entityType:entityId
        return String.format("cnt:%s:%s:%s", CounterSchema.SCHEMA_ID, entityType, entityId);
    }

    // bitmap key format: bm:metric:entityType:entityId:chunk
    public static String bitmapKey(String metric, String entityType, String entityId, long chunk){
        return String.format("bm:%s:%s:%s:%d", metric, entityType, entityId, chunk);
    }

    // Aggregated counter key format(Hash): agg:v1:entityType:entityId
    public static String aggKey(String entityType, String entityId){
        return String.format("agg:%s:%s:%s",CounterSchema.SCHEMA_ID, entityType, entityId);
    }
}
