package com.star.share.posts.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.star.share.posts.entity.model.Post;
import com.star.share.counter.event.CounterEvent;
import com.star.share.posts.entity.vo.FeedItemResponse;
import com.star.share.posts.entity.vo.FeedPageResponse;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.event.EventListener;

public class FeedCacheInvalidationListener {
    private final Cache<String, FeedPageResponse> feedPublicCache;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final com.star.share.counter.service.UserCounterService userCounterService;
    private final com.star.share.posts.dao.PostMapper postMapper;

    public FeedCacheInvalidationListener(@Qualifier("feedPublicCache") Cache<String, FeedPageResponse> feedPublicCache,
                                         StringRedisTemplate redis,
                                         ObjectMapper objectMapper,
                                         com.star.share.counter.service.UserCounterService userCounterService,
                                         com.star.share.posts.dao.PostMapper postMapper) {
        this.feedPublicCache = feedPublicCache;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.userCounterService = userCounterService;
        this.postMapper = postMapper;
    }

    /**
     * Listener for counter change events that affect feed page caches.
     *
     * <p>processing：</p>
     *
     * - Only process like/fav events for entity type "post";
     * - If the creator ID of the content can be resolved, synchronize their "likes/favs received" counts;
     * - Use the reverse index sets from the last two hours to locate affected pages:
     *  - Update local Caffeine page cache (preserving liked/faved flags);
     *  - Update Redis page cache (without user state flags, keeping TTL).
     * - If a page key is not found in Redis, clean up its index reference to reduce keyspace noise.
     */
    @EventListener
    public void onCounterChanged(CounterEvent event) {
        if (!"knowpost".equals(event.getEntityType())) {
            return;
        }

        String metric = event.getMetric();
        if ("like".equals(metric) || "fav".equals(metric)) {
            String eid = event.getEntityId();
            int delta = event.getDelta();

            try {
                Post post = postMapper.findById(Long.valueOf(eid));
                if (post != null && post.getCreatorId() != null) {
                    long owner = post.getCreatorId();
                    if ("like".equals(metric)) {
                        userCounterService.incrementLikesReceived(owner, delta);
                    }
                    if ("fav".equals(metric)) {
                        userCounterService.incrementFavsReceived(owner, delta);
                    }
                }
            } catch (Exception ignored) {
            }

            long hourSlot = System.currentTimeMillis() / 3600000L;
            Set<String> keys = new LinkedHashSet<>();
            Set<String> cur = redis.opsForSet().members("feed:public:index:" + eid + ":" + hourSlot);
            if (cur != null) {
                keys.addAll(cur);
            }

            Set<String> prev = redis.opsForSet().members("feed:public:index:" + eid + ":" + (hourSlot - 1));
            if (prev != null) {
                keys.addAll(prev);
            }
            if (keys.isEmpty()) {
                return;
            }

            for (String key : keys) {
                FeedPageResponse local = feedPublicCache.getIfPresent(key);
                if (local != null) {
                    FeedPageResponse updatedLocal = adjustPageCounts(local, eid, metric, delta, true);
                    feedPublicCache.put(key, updatedLocal);
                }

                String cached = redis.opsForValue().get(key);
                if (cached != null) {
                    try {
                        FeedPageResponse resp = objectMapper.readValue(cached, FeedPageResponse.class);
                        FeedPageResponse updated = adjustPageCounts(resp, eid, metric, delta, false);
                        writePageJsonKeepingTtl(key, updated);
                    } catch (Exception ignored) {}
                } else {
                    redis.opsForSet().remove("feed:public:index:" + eid + ":" + hourSlot, key);
                }
            }
        }
    }

    /**
     * Adjust the like/fav counts of a feed page response for a specific item ID.
     *
     * <p>Behavior:</p>
     * - Iterate through the page items, locate the item with id==eid, and update like/fav counts;
     * - preserveUserFlags=true: retain liked/faved flags for local cache;
     * - preserveUserFlags=false: do not include user state flags when writing back to Redis page JSON;
     * - Return a new snapshot of the page response.
     */
    private FeedPageResponse adjustPageCounts(FeedPageResponse page, String eid, String metric, int delta, boolean preserveUserFlags) {
        List<FeedItemResponse> items = new ArrayList<>(page.items().size());
        for (FeedItemResponse it : page.items()) {
            if (eid.equals(it.id())) {
                Long like = it.likeCount();
                Long fav = it.favoriteCount();

                if ("like".equals(metric)) {
                    like = Math.max(0L, (like == null ? 0L : like) + delta);
                }
                if ("fav".equals(metric)) {
                    fav = Math.max(0L, (fav == null ? 0L : fav) + delta);
                }

                Boolean liked = preserveUserFlags ? it.liked() : null;
                Boolean faved = preserveUserFlags ? it.faved() : null;

                it = new FeedItemResponse(
                        it.id(),
                        it.title(),
                        it.description(),
                        it.coverImage(),
                        it.tags(),
                        it.authorAvatar(),
                        it.authorNickname(),
                        it.tagJson(),
                        like,
                        fav,
                        liked,
                        faved,
                        it.isTop()
                );
            }
            items.add(it);
        }

        return new FeedPageResponse(items, page.page(), page.size(), page.hasMore());
    }

    /**
     * Write the updated page JSON back to Redis, preserving the original TTL if it exists.
     *
     * <p>Behavior:</p>
     * - Preserve the original TTL of the page cache to avoid resetting it due to overwrite;
     * - If the key does not have a TTL, write the latest JSON directly.
     */
    private void writePageJsonKeepingTtl(String key, FeedPageResponse page) {
        try {
            String json = objectMapper.writeValueAsString(page);
            long ttl = redis.getExpire(key);
            if (ttl > 0) {
                redis.opsForValue().set(key, json, java.time.Duration.ofSeconds(ttl));
            } else {
                redis.opsForValue().set(key, json);
            }
        } catch (Exception ignored) {}
    }
}
