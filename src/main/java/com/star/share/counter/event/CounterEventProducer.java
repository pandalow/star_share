package com.star.share.counter.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CounterEventProducer {
    private final KafkaTemplate<String, String> kafkaRedisTemplate;
    private final ObjectMapper objectMapper;

    public CounterEventProducer(KafkaTemplate<String, String> kafkaRedisTemplate, ObjectMapper objectMapper) {
        this.kafkaRedisTemplate = kafkaRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish a counter event to the Kafka topic.
     * @param event The counter event to publish.
     */
    public void publish(CounterEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaRedisTemplate.send(CounterTopics.EVENTS, eventJson);
        } catch (Exception e) {
            log.warn("Failed to publish counter event: {}", e.getMessage());
        }

    }
}
