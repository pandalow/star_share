package com.star.share.counter.event;

import lombok.Data;

/**
 * Base class for counter-related events
 *
 * Describe the one state change event(eg: like, favorite)
 * Through Producer send to Kafka, Consumer aggregate the event to update the
 * counter in Redis
 */
@Data
public class CounterEvent {
    private String entityType;
    private String entityId;
    private String metric; // like | fav（指标名称）
    private int idx; // schema index（见 CounterSchema.NAME_TO_IDX）
    private long userId;
    private int delta; // +1 / -1

    public CounterEvent(String entityType, String entityId, String metric, int idx, long userId, int delta) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.metric = metric;
        this.idx = idx;
        this.userId = userId;
        this.delta = delta;
    }

    public static CounterEvent of(String entityType, String entityId, String metric, int idx, long userId, int delta) {
        return new CounterEvent(entityType, entityId, metric, idx, userId, delta);
    }
}