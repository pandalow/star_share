package com.star.share.relation.processer;

import com.star.share.counter.service.UserCounterService;
import com.star.share.relation.entity.RelationEvent;
import com.star.share.relation.mapper.RelationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Processor for handling relation events such as follow creation and cancellation.
 * This class ensures that each event is processed only once by using Redis for deduplication.
 * It updates the follower/following relationships in the database
 * and maintains the corresponding counts in Redis. */
@Service
@RequiredArgsConstructor
public class RelationEventProcessor {
    private final RelationMapper mapper;
    private final StringRedisTemplate redis;
    private final UserCounterService userCounterService;


    /**
     * Processes a relation event by updating the follower/following relationships
     * and counts. * Uses Redis to ensure that each event is processed only once,
     * preventing duplicate processing.
     */
    public void process(RelationEvent event) {
        String defaultkey = "dedup:rel:"
                + event.type() + ":" + event.fromUserId() + ":" + event.toUserId()
                + ":" + event.id() == null ? "0" : String.valueOf(event.id());
        Boolean first = redis.opsForValue().setIfAbsent(defaultkey, "1", Duration.ofMinutes(10));

        if (first == null || !first) {
            // Already processed this event, skip it
            return;
        }

        if ("FollowCreated".equals(event.type())) {
            mapper.insertFollower(event.id(), event.toUserId(), event.fromUserId(), 1);

            long now = System.currentTimeMillis();
            // Use sorted sets to store followers and followings with timestamps for potential future use (e.g., sorting by follow time)
            redis.opsForZSet().add("uf:flws:" + event.fromUserId(), String.valueOf(event.toUserId()), now);
            redis.opsForZSet().add("uf:fans" + event.toUserId(), String.valueOf(event.fromUserId()), now);
            redis.expire("uf:flws:" + event.fromUserId(), Duration.ofHours(2));
            redis.expire("uf:fans:" + event.toUserId(), Duration.ofHours(2));

            userCounterService.incrementFollowings(event.fromUserId(), 1);
            userCounterService.incrementFollowers(event.toUserId(), 1);

        } else if ("FollowCanceled".equals(event.type())) {
            mapper.cancelFollower(event.toUserId(), event.fromUserId());
            // Remove the follower/following relationship from Redis sorted sets
            redis.opsForZSet().remove("uf:flws:" + event.fromUserId(), String.valueOf(event.toUserId()));
            redis.opsForZSet().remove("uf:fans" + event.toUserId(), String.valueOf(event.fromUserId()));
            // No need to set expiration here since the entries are removed immediately
            userCounterService.incrementFollowings(event.fromUserId(), -1);
            userCounterService.incrementFollowers(event.toUserId(), -1);
        }
    }


}
