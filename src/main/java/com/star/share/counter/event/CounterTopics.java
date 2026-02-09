package com.star.share.counter.event;

/**
 * Defines Kafka topic names for counter-related events.
 * This class centralizes topic definitions to ensure consistency across the application.
 */
public class CounterTopics {
    public static final String EVENTS = "counter-events";
    private CounterTopics() {
        // Prevent instantiation
    }
}
