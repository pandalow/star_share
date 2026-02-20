package com.star.share.counter.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.star.share.counter.schema.CounterSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.star.share.counter.schema.CounterKeys;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Consumer that listens to counter events from Kafka, aggregates them in Redis, and periodically flushes the aggregated counts to the final counter storage.
 * The consumer uses a Lua script to ensure atomic increments of the counters in Redis and handles message acknowledgment to ensure reliable processing.
 */
@Service
@Slf4j
public class CounterAggregationConsumer {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> incrScript;

    public CounterAggregationConsumer(ObjectMapper objectMapper,
                                      StringRedisTemplate redis) {
        this.objectMapper = objectMapper;
        this.redis = redis;
        this.incrScript = new DefaultRedisScript<>();
        this.incrScript.setResultType(Long.class);
        this.incrScript.setScriptText(INCR_FIELD_LUA);
    }

    /**
     * Kafka listener ming a Lua script for atomic increments,
     *
     * @param message The incoming message from Kafka, expected to be a JSON string representing a CounterEvent.
     * @param ack     Acknowledgment object for manual acknowledgment of message processing. The message will only be acknowledged if processed successfully.
     */
    @KafkaListener(topics = CounterTopics.EVENTS, groupId = "counter-agg")
    public void onMessage(String message, Acknowledgment ack) throws JsonProcessingException {
        CounterEvent evt = objectMapper.readValue(message, CounterEvent.class);
        String aggKey = CounterKeys.aggKey(evt.getEntityType(), evt.getEntityId());
        // idx : 0,1,2,3 , 1 means like, 2 means favorite
        String field = String.valueOf(evt.getIdx());
        try {
            // Use Lua script to atomically increment the specific field in the hash
            redis.opsForHash().increment(aggKey, field, evt.getDelta());
            // Successfully processed the message, acknowledge it
            ack.acknowledge();
        } catch (Exception e) {
            // Log the error and do not acknowledge, so it can be retried
            log.warn("Failed to process counter event: {}, error: {}", evt, e.getMessage());
        }
    }

    /**
     * Scheduled task to flush aggregated counters from Redis hashes to the final counter storage.
     * This method runs every second, retrieves all aggregation keys, and for each key,
     * it processes the fields and increments the final counters accordingly.
     */
    @Scheduled(fixedDelay = 1000L)
    public void flush() {
        // keys: agg:v1:1:
        Set<String> keys = redis.keys("agg:" + CounterSchema.SCHEMA_ID + ":");
        if (keys.isEmpty()) {
            return;
        }

        for (String aggkey : keys) {
            // Get all fields and their corresponding deltas from the agg:v1:* hash
            Map<Object, Object> entries = redis.opsForHash().entries(aggkey);
            if (entries.isEmpty()) {
                continue;
            }
            String[] parts = aggkey.split(":", 4);
            if (parts.length < 4) {
                log.warn("Invalid agg key format: {}", aggkey);
                continue;
            }
            // cnt:v1:entityType:entityId
            String cntKey = CounterKeys.sdsKey(parts[2], parts[3]);
            for (Map.Entry<Object, Object> e : entries.entrySet()) {
                String field = String.valueOf(e.getKey());

                long delta;
                try {
                    delta = Long.parseLong(String.valueOf(e.getValue()));
                } catch (NumberFormatException ex) {
                    log.warn("Invalid counter value for key: {}, field: {}, value: {}", aggkey, field, e.getValue());
                    continue;
                }
                if (delta == 0) continue;

                int idx;
                try {
                    idx = Integer.parseInt(field);
                } catch (NumberFormatException nfe) {
                    continue;
                }

                try {
                    redis.execute(incrScript,
                            List.of(cntKey),
                            String.valueOf(CounterSchema.SCHEMA_LEN),
                            String.valueOf(CounterSchema.FIELD_SIZE),
                            String.valueOf(idx),
                            String.valueOf(delta)
                    );

                    redis.opsForHash().delete(aggkey, field);
                } catch (Exception ex) {
                    log.warn("Failed to flush counter for key: {}, field: {}, delta: {}, error: {}",
                            cntKey, field, delta, ex.getMessage());
                }
            }

            Long size = redis.opsForHash().size(aggkey);
            if (size == 0L) {
                redis.delete(aggkey);
            }
        }
    }

    private static final String INCR_FIELD_LUA = """
                        
            local cntKey = KEYS[1]
            local schemaLen = tonumber(ARGV[1])
            local fieldSize = tonumber(ARGV[2]) -- 固定为4
            local idx = tonumber(ARGV[3])
            local delta = tonumber(ARGV[4])
                        
            local function read32be(s, off)
              local b = {string.byte(s, off+1, off+4)}
              local n = 0
              for i=1,4 do n = n * 256 + b[i] end
              return n
            end
                        
            local function write32be(n)
              local t = {}
              for i=4,1,-1 do t[i] = n % 256; n = math.floor(n/256) end
              return string.char(unpack(t))
            end
                        
            local cnt = redis.call('GET', cntKey)
            if not cnt then cnt = string.rep(string.char(0), schemaLen * fieldSize) end
            local off = idx * fieldSize
            local v = read32be(cnt, off) + delta
            if v < 0 then v = 0 end
            local seg = write32be(v)
            cnt = string.sub(cnt, 1, off) .. seg .. string.sub(cnt, off+fieldSize+1)
            redis.call('SET', cntKey, cnt)
            return 1
            """;
}


