package com.star.share.counter.event;

import lombok.Data;

/**
 * Base class for counter-related events
 *
 * Describe the one state change event(eg: like, favorite)
 * Through Producer send to Kafka, Consumer aggregate the event to update the counter in Redis
 */
@Data
public class CounterEvent {

    private String entityType;
    private String entityId;   // e.g., "post123", "comment456"
    private String metric;     // e.g., "like", "favorite"
    private int idx; // schema index ( see: CounterSchema. NAME_TO_IDX)
    private long userId; // the user who performed the action
    private int delta; // +1 / -1 for increment/decrement

    public CounterEvent(
            String entityType,
            String entityId,
            String metric,
            int idx,
            long userId,
            int delta
    ) {
        // Default constructor for deserialization
        this.entityType = entityType;
        this.entityId = entityId;
        this.metric = metric;
        this.idx = idx;
        this.userId = userId;
        this.delta = delta;
    }

    /**
     * Factory method to create a CounterEvent instance.
     * @param entityType
     * @param entityId
     * @param metric
     * @param idx
     * @param userId
     * @param delta
     * @return
     */
    public static CounterEvent of(String entityType, String entityId, String metric, int idx, long userId, int delta) {
        return new CounterEvent(entityType, entityId, metric, idx, userId, delta);
    }
}
