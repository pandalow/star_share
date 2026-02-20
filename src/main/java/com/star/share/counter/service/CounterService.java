package com.star.share.counter.service;

import java.util.List;
import java.util.Map;

public interface CounterService {
    /**
     * like operation for entity, increase the like count of the entity by 1.
     */
    boolean like(String entityType, String entityId, long uid);

    boolean isLiked(String entityType, String entityId, long uid);

    boolean unlike(String entityType, String entityId, long uid);

    boolean fav(String entityType, String entityId, long uid);

    boolean isFaved(String entityType, String entityId, long uid);

    boolean unfav(String entityType, String entityId, long uid);

    Map<String, Long> getCounts(String entityType, String entityId, List<String> metrics);
}
