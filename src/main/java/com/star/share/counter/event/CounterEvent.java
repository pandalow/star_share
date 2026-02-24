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
    
     * @param delta
     * @return
     */
    public static CounterEvent of(String entityType, String entityId, String metric, int idx, long userId, int delta) {
        return new CounterEvent(entityType, entityId, metric, idx, userId, delta);
    }
}
