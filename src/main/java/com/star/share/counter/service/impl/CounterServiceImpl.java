package com.star.share.counter.service.impl;

import com.star.share.counter.event.CounterEvent;
import com.star.share.counter.event.CounterEventProducer;
import com.star.share.counter.schema.BitmapShard;
import com.star.share.counter.schema.CounterKeys;
import com.star.share.counter.schema.CounterSchema;
import com.star.share.counter.service.CounterService;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.w3c.dom.css.Counter;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CounterServiceImpl implements the CounterService interface
 * to provide like and favorite functionalities for entities such as posts and comments.
 */
@Service
public class CounterServiceImpl implements CounterService {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> toggleScript;
    private final CounterEventProducer eventProducer;
    private final ApplicationEventPublisher eventPublisher;
    private final RedissonClient redisson;


    public CounterServiceImpl(StringRedisTemplate redis,
                              CounterEventProducer eventProducer,
                              ApplicationEventPublisher eventPublisher,
                              RedissonClient redisson) {
        this.redis = redis;
        this.eventProducer = eventProducer;
        this.eventPublisher = eventPublisher;
        this.redisson = redisson;

        this.toggleScript = new DefaultRedisScript<>();
        this.toggleScript.setResultType(Long.class);
        // Switch to Lua script to ensure atomicity of the like/unlike operation
        this.toggleScript.setScriptText(TOGGLE_LUA);

    }

    /**
     * like operation: Bitmap toggle, return True when unlike -> like;
     * Synchronously publish the event to Kafka
     *
     * @param entityType entity type (eg: post, comment)
     * @param entityId   entity id (eg: post123, comment456)
     * @param uid        user id
     * @return true if the like status changed to liked.
     */
    @Override
    public boolean like(String entityType, String entityId, long uid) {
        return toggle(entityType, entityId, uid, "like", CounterSchema.IDX_LIKE, true);
    }

    /**
     * unlike operation: Bitmap toggle, return True when like -> unlike;
     *
     * @param entityType entity type (eg: post, comment)
     * @param entityId   entity id (eg: post123, comment456)
     * @param uid        user id
     */
    @Override
    public boolean unlike(String entityType, String entityId, long uid) {
        return toggle(entityType, entityId, uid, "like", CounterSchema.IDX_LIKE, false);
    }

    /**
     * favorite operation: Bitmap toggle, return True when unfavorite -> favorite;
     *
     * @param entityType
     * @param entityId
     * @param uid
     * @return
     */
    @Override
    public boolean fav(String entityType, String entityId, long uid) {
        return toggle(entityType, entityId, uid, "fav", CounterSchema.IDX_FAV, true);
    }

    /**
     * unfavorite operation: Bitmap toggle, return True when favorite -> unfavorite;
     *
     * @param entityType
     * @param entityId
     * @param uid
     * @return
     */
    @Override
    public boolean unfav(String entityType, String entityId, long uid) {
        return toggle(entityType, entityId, uid, "fav", CounterSchema.IDX_FAV, false);
    }


    /**
     * Get the counts for the specified metrics of an entity by reading the raw byte array from Redis and parsing the values based on the defined schema.
     * @param entityType entity type (eg: post, comment)
     * @param entityId  entity id (eg: post123, comment456)
     * @param metrics list of metrics to retrieve (eg: like, favorite)
     * @return a map of metric name to count value for the specified metrics. If a metric is not found in the schema, it will be skipped.
     */
    @Override
    public Map<String, Long> getCounts(String entityType, String entityId, List<String> metrics) {
        String sdsKey = CounterKeys.sdsKey(entityType, entityId);
        byte[] raw = getRaw(sdsKey);

        // TODO: Adding the distributed lock to prevent the cache penetration when the key is not exist in Redis, and the backend database is down or has no record for the entity, which will cause a lot of cache miss and hit the database repeatedly.
        Map<String, Long> result = new LinkedHashMap<>();

        for(String m : metrics){
            Integer idx = CounterSchema.NAME_TO_IDX.get(m);
            if(idx == null){
                continue;
            }
            int off = idx * CounterSchema.FIELD_SIZE;
            long val = readInt32BE(raw, off);
            result.put(m, val);
        }
        return result;
    }

    /**
     * Check if the user has favorite the entity by checking the corresponding bit in Redis bitmap.
     *
     * @param entityType entity type (eg: post, comment)
     * @param entityId   entity id (eg: post123, comment456)
     * @param uid        user id
     * @return true if the user has favorite the entity, false otherwise
     */
    @Override
    public boolean isFaved(String entityType, String entityId, long uid) {
        long chunk = BitmapShard.chunkOf(uid);
        long bit = BitmapShard.bitOf(uid);
        return getBit(CounterKeys.bitmapKey("fav", entityType, entityId, chunk), bit);
    }


    /**
     * Check if the user has liked the entity by checking the corresponding bit in Redis bitmap.
     *
     * @param entityType entity type (eg: post, comment)
     * @param entityId   entity id (eg: post123, comment456)
     * @param uid        user id
     * @return true if the user has liked the entity, false otherwise
     */
    @Override
    public boolean isLiked(String entityType, String entityId, long uid) {
        long chunk = BitmapShard.chunkOf(uid);
        long bit = BitmapShard.bitOf(uid);
        return getBit(CounterKeys.bitmapKey("like", entityType, entityId, chunk), bit);

    }

    /**
     * Reading the raw byte array from Redis for the given key.
     * @param key the Redis key to read
     * @return  the raw byte array value associated with the key, or null if the key does not exist
     */
    private byte[] getRaw(String key){
        return redis.execute((RedisCallback<byte[]>) connection ->
                connection.stringCommands().get(key.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Get the bit value at the specified offset for the given key in Redis.
     *
     * @param key    the Redis key for the bitmap
     * @param offset the bit offset to check
     * @return true if the bit is set (1), false if the bit is not set (0)
     */
    private boolean getBit(String key, long offset) {
        Boolean bit = redis.execute((RedisCallback<Boolean>) connection ->
                connection.stringCommands().getBit(key.getBytes(StandardCharsets.UTF_8), offset));
        return Boolean.TRUE.equals(bit);
    }

    /**
     * Read a 32-bit big-endian integer from the given byte array starting at the specified offset.
     */
    private static long readInt32BE(byte[] data, int offset){
        long n = 0;
        for(int i = 0; i <4 ; i++){
            n = (n << 8) |(data[offset + i] & 0xFFL);
        }
        return n;
    }

    /**
     * Bitmap toggle operation: Only return ok if changed status, otherwise return false
     *
     * @param etype  entity type
     * @param eid    entity id
     * @param uid    user id
     * @param metric counter metric (eg: like, favorite)
     * @param idx    counter schema index (see: CounterSchema. NAME_TO_IDX)
     * @param add    true for like/add, false for unlike/remove
     */
    private boolean toggle(String etype,
                           String eid,
                           long uid,
                           String metric,
                           int idx,
                           boolean add) {
        long chunk = BitmapShard.chunkOf(uid);

        // Bitmap offset for the user id
        long bit = BitmapShard.bitOf(uid);
        String bmKey = CounterKeys.bitmapKey(metric, etype, eid, chunk);
        List<String> keys = List.of(bmKey);
        List<String> args = List.of(String.valueOf(bit), add ? "add" : "remove");

        Long changed = redis.execute(toggleScript, keys, args.toArray());
        if (changed == null) {
            // Redis error or timeout; treat as no change
            return false;
        }

        boolean ok = changed == 1L;
        if (ok) {
            int delta = add ? 1 : -1;

            eventProducer.publish(CounterEvent.of(
                    etype, eid, metric, idx, uid, delta
            ));

            eventPublisher.publishEvent(CounterEvent.of(
                    etype, eid, metric, idx, uid, delta
            ));
        }
        return ok;
    }

    // LUA SCRIPT to toggle the like status of a user for an entity
    private static final String TOGGLE_LUA = """
            local bmKey = KEYS[1]
            local offset = tonumber(ARGV[1])
            local op = ARGV[2] -- 'add' or 'remove'
            local prev = redis.call('GETBIT', bmKey, offset)
            if op == 'add' then
              if prev == 1 then return 0 end
              redis.call('SETBIT', bmKey, offset, 1)
              return 1
            elseif op == 'remove' then
              if prev == 0 then return 0 end
              redis.call('SETBIT', bmKey, offset, 0)
              return 1
            end
            return -1
            """;
}
