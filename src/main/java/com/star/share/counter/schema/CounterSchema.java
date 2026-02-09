package com.star.share.counter.schema;

import java.util.Map;
import java.util.Set;

/**
 * Defines the schema for counters, including field sizes and mappings.
 * V1 version using 4 bytes INT 32 for counters(SDS), can replace with 5 bytes INT 40 if needed.
 */
public class CounterSchema {
    // 0: read
    // 1: like
    // 2: fav
    // 3: comment(presumed to be 0 for now, can be used for future comment count)
    // 4: repost(presumed to be 0 for now, can be used for future repost count)
    public static final String SCHEMA_ID = "v1";
    public static final int FIELD_SIZE = 4; // 4 bytes INT 32
    public static final int SCHEMA_LEN = 5; // Save 5 fields;

    public static final int IDX_LIKE = 1;
    public static final  int IDX_FAV = 2;
    public static final Map<String , Integer> NAME_TO_IDX = Map.of(
            "like", IDX_LIKE,
            "fav", IDX_FAV
    );

    public static final Set<String> SUPPORTED_COUNTERS = NAME_TO_IDX.keySet();

    private CounterSchema() {
        // Prevent instantiation
    }
}
