package com.star.share.counter.entity;

import lombok.Data;

import java.util.Map;

@Data
public class CountsResponse {
    private String entityType;
    private String entityId;
    private Map<String, Long> counts;

    /**
     * Constructor for CountsResponse.
     * @param entityType the type of the entity (e.g., "post", "comment")
     * @param entityId the unique identifier of the entity
     * @param counts a map containing the counts for various actions (e.g., "likes", "shares") associated with the entity
     */
    public CountsResponse(String entityType, String entityId, Map<String, Long> counts) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.counts = counts;
    }
}
