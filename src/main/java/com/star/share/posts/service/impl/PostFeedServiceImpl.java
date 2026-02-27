package com.star.share.posts.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.star.share.cache.hotkey.HotKeyDetector;
import com.star.share.counter.service.CounterService;
import com.star.share.posts.dao.PostMapper;
import com.star.share.posts.entity.model.PostFeedRow;
import com.star.share.posts.entity.vo.FeedItemResponse;
import com.star.share.posts.entity.vo.FeedPageResponse;
import com.star.share.posts.service.PostFeedService;

import lombok.extern.slf4j.Slf4j;

/**
 * Post feed service with multi-level caching (Local+Redis), stampede
 * prevention, and hot-key management.
 */
@Service
@Slf4j
public class PostFeedServiceImpl implements PostFeedService {

    private final int LAYOUT_VERSION = 1;
    private final Cache<String, FeedPageResponse> feedPublicCache;
    private final Cache<String, FeedPageResponse> feedMineCache;
    private final ConcurrentHashMap<String, Object> singleFlight = new ConcurrentHashMap<>();
    private final PostMapper postMapper;
    private final CounterService counterService;
    private final HotKeyDetector hotKey;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public PostFeedServiceImpl(
            PostMapper mapper,
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            CounterService counterService,
            @Qualifier("feedPublicCache") Cache<String, FeedPageResponse> feedPublicCache,
            @Qualifier("feedMineCache") Cache<String, FeedPageResponse> feedMineCache,
            HotKeyDetector hotKey) {
        this.postMapper = mapper;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.counterService = counterService;
        this.feedPublicCache = feedPublicCache;
        this.feedMineCache = feedMineCache;
        this.hotKey = hotKey;
    }

    /**
     * Retrieves paginated public feed.
     * Uses L1 (Local) + L2 (Redis) caching logic with SingleFlight pattern to
     * prevent cache stampede.
     *
     * @param page                  page number (1-based)
     * @param size                  page size
     * @param currentUserIdNullable current user ID for personalization (like/fav
     *                              status)
     */
    @Override
    public FeedPageResponse getFeed(int page, int size, Long currentUserIdNullable) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 1);

        String localPageKey = cacheKey(safePage, safeSize);
        long hourSlot = System.currentTimeMillis() / 3600000L;

        String idsKey = "feed:public:ids:" + safeSize + ":" + hourSlot + ":" + safePage;
        String hasMoreKey = "feed:public:ids:" + safeSize + ":" + hourSlot + ":" + safePage + ":hasMore";

        // L1 : Using local cache to prevent cache stampede, with a short TTL of 60
        // seconds;
        FeedPageResponse localCache = feedPublicCache.getIfPresent(localPageKey);

        if (localCache != null && localCache.items() != null) {
            for (FeedItemResponse item : localCache.items()) {
                recordItemHotKey(item.id());
            }
            log.info("feed.public source=local localPageKey={} page={} size={}", localPageKey, safePage, safeSize);
            List<FeedItemResponse> enrichedLocal = enrich(localCache.items(), currentUserIdNullable);

            return new FeedPageResponse(enrichedLocal, localCache.page(), localCache.size(), localCache.hasMore());
        }

        // L2 : Using Redis
        FeedPageResponse redisCache = assembleFromCache(idsKey, hasMoreKey, safePage, safeSize, currentUserIdNullable);
        if (redisCache != null) {
            feedPublicCache.put(localPageKey, redisCache);
            if (redisCache.items() != null) {
                for (FeedItemResponse item : redisCache.items()) {
                    recordItemHotKey(item.id());
                }
            }
            log.info("feed.public source=redis idsKey={} hasMoreKey={} page={} size={}", idsKey, hasMoreKey, safePage,
                    safeSize);
            return redisCache;
        }

        Object lock = singleFlight.computeIfAbsent(idsKey, k -> new Object());

        try {
            synchronized (lock) {
                // Double check after acquiring lock
                FeedPageResponse again = assembleFromCache(idsKey, hasMoreKey, safePage, safeSize,
                        currentUserIdNullable);
                if (again != null) {
                    feedPublicCache.put(localPageKey, again);

                    if (again.items() != null) {
                        for (FeedItemResponse item : again.items()) {
                            recordItemHotKey(item.id());
                        }
                    }
                    log.info(
                            "feed.public source=3tier(after-flight) localPageKey={} idsKey={} hasMoreKey={} page={} size={}",
                            localPageKey, idsKey, hasMoreKey, safePage, safeSize);
                    return again;
                }

                // Database retrieval,
                int offset = (safePage - 1) * safeSize;
                List<PostFeedRow> rows = postMapper.listFeedPublic(safeSize + 1, offset);
                boolean hasMore = rows.size() > safeSize;
                if (hasMore) {
                    rows = rows.subList(0, safeSize);
                }

                // Building cache and response
                List<FeedItemResponse> items = mapRowsToItems(rows, null, false);

                FeedPageResponse responseForCache = new FeedPageResponse(items, safePage, safeSize, hasMore);

                // Segments of cache keys;
                int baseTtl = 60;
                int jitter = ThreadLocalRandom.current().nextInt(30);
                Duration frTtl = Duration.ofSeconds(baseTtl + jitter);

                writeToCache(localPageKey, idsKey, hasMoreKey, safeSize, rows, items, hasMore, frTtl);
                feedPublicCache.put(localPageKey, responseForCache);

                List<FeedItemResponse> enriched = enrich(items, currentUserIdNullable);
                return new FeedPageResponse(enriched, safePage, safeSize, hasMore);
            }       
        } finally {
            singleFlight.remove(idsKey);
        }

    }

    /**
     * Retrieves personal feed ("My Posts") with simple Redis caching.
     */
    @Override
    public FeedPageResponse getMyFeed(long userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 1);
        String key = cacheKey(safePage, safeSize);

        FeedPageResponse localCache = feedMineCache.getIfPresent(key);
        if (localCache != null) {
            hotKey.record(key);
            maybeExtendTtlMine(key);
            return localCache;
        }

        String redisCached = redis.opsForValue().get(key);
        if (redisCached != null) {
            try {
                FeedPageResponse cached = objectMapper.readValue(redisCached, FeedPageResponse.class);
                boolean hasCounts = cached.items() != null
                        && cached.items().stream().allMatch(it -> it.likeCount() != null && it.favoriteCount() != null);
                if (hasCounts) {
                    feedMineCache.put(key, cached);
                    hotKey.record(key);
                    maybeExtendTtlMine(key);
                    List<FeedItemResponse> enriched = enrich(cached.items(), userId);
                    return new FeedPageResponse(enriched, cached.page(), cached.size(), cached.hasMore());
                }
            } catch (Exception e) {
                // Ignore cache parsing errors
            }
        }

        int offset = (safePage - 1) * safeSize;
        List<PostFeedRow> rows = postMapper.listMyPublished(userId, safeSize + 1, offset);
        boolean hasMore = rows.size() > safeSize;
        if (hasMore) {
            rows = rows.subList(0, safeSize);
        }

        List<FeedItemResponse> items = mapRowsToItems(rows, userId, true);

        FeedPageResponse response = new FeedPageResponse(items, safePage, safeSize, hasMore);
        try {
            String json = objectMapper.writeValueAsString(response);
            int baseTtl = 60;
            int jitter = ThreadLocalRandom.current().nextInt(20);

            redis.opsForValue().set(key, json, Duration.ofSeconds(baseTtl + jitter));
            feedMineCache.put(key, response);
            hotKey.record(key);
        } catch (Exception e) {
            // Ignore cache write errors
        }

        return response;
    }

    // Helper methods for cache assembly, enrichment, and mapping rows to objects.
    private FeedPageResponse assembleFromCache(String idsKey, String hasMoreKey, int safePage, int safeSize,
            Long uid) {
        List<String> idList = redis.opsForList().range(idsKey, 0, safeSize - 1);
        String hasMoreStr = redis.opsForValue().get(hasMoreKey);
        if (idList == null || idList.isEmpty()) {
            return null;
        }
        // Construct cache keys for batch retrieval
        List<String> itemKeys = new ArrayList<>(idList.size());
        for (String id : idList) {
            itemKeys.add("feed:item:" + id);
        }
        // Batch get from Redis
        List<String> itemJsons = redis.opsForValue().multiGet(itemKeys);

        List<FeedItemResponse> items = new ArrayList<>(idList.size());

        for (int i = 0; i < idList.size(); i++) {
            String itemJson = (itemJsons != null && i < itemJsons.size()) ? itemJsons.get(i) : null;
            if (itemJson == null) {
                //
                return null;
            }

            try {
                items.add(objectMapper.readValue(itemJson, FeedItemResponse.class));
            } catch (Exception e) {
                return null;
            }
        }

        List<FeedItemResponse> enriched = new ArrayList<>(idList.size());
        for (int i = 0; i < idList.size(); i++) {
            FeedItemResponse base = items.get(i);
            if (base == null) {
                continue;
            }

            Map<String, Long> counts = counterService.getCounts("post", String.valueOf(base.id()),
                    List.of("like", "fav"));
            Long likeCount = counts.getOrDefault("like", 0L);
            Long favoriteCount = counts.getOrDefault("fav", 0L);

            // Determine if the current user has liked or favorited this post
            boolean liked = uid != null && counterService.isLiked("post", base.id(), uid);
            boolean faved = uid != null && counterService.isFaved("post", base.id(), uid);

            enriched.add(new FeedItemResponse(
                    base.id(),
                    base.title(),
                    base.description(),
                    base.coverImage(),
                    base.tags(),
                    base.authorAvatar(),
                    base.authorNickname(),
                    base.tagJson(),
                    likeCount,
                    favoriteCount,
                    liked,
                    faved,
                    base.isTop()));
        }
        // If hasMoreStr is not present, we can infer hasMore based on whether we got a
        // full page of results
        boolean hasMore = hasMoreStr != null ? "1".equals(hasMoreStr) : (idList.size() == safeSize);

        return new FeedPageResponse(enriched, safePage, safeSize, hasMore);
    }

    private void recordItemHotKey(String itemId) {
        String hotKeyId = "post:" + itemId;
        hotKey.record(hotKeyId);

        int baseTtl = 60;
        int target = hotKey.ttlForPublic(baseTtl, hotKeyId);

        // Extend TTL in Redis if this item is becoming hot, to prevent it from being
        // evicted from cache
        String itemKey = "feed:item:" + itemId;
        Long itemTtl = redis.getExpire(itemKey);
        if (itemTtl < target) {
            redis.expire(itemKey, Duration.ofSeconds(target));
        }
    }

    /**
     * Enriches items with user-specific status (liked/faved).
     */
    private List<FeedItemResponse> enrich(List<FeedItemResponse> base, Long uid) {
        List<FeedItemResponse> out = new ArrayList<>(base.size());

        for (FeedItemResponse it : base) {
            boolean liked = uid != null && counterService.isLiked("post", it.id(), uid);
            boolean faved = uid != null && counterService.isFaved("post", it.id(), uid);
            out.add(new FeedItemResponse(
                    it.id(),
                    it.title(),
                    it.description(),
                    it.coverImage(),
                    it.tags(),
                    it.authorAvatar(),
                    it.authorNickname(),
                    it.tagJson(),
                    it.likeCount(),
                    it.favoriteCount(),
                    liked,
                    faved,
                    it.isTop()));
        }
        return out;
    }

    /**
     * Converts DB rows to feed items and enriches with counter service data.
     *
     * @param rows           DB result rows
     * @param userIdNullable current user ID (for liked/faved check)
     * @param includeIsTop   whether to populate 'isTop' field
     * @return enriched feed items
     */
    private List<FeedItemResponse> mapRowsToItems(List<PostFeedRow> rows, Long userIdNullable, boolean includeIsTop) {
        List<FeedItemResponse> items = new ArrayList<>(rows.size());

        for (PostFeedRow r : rows) {
            List<String> tags = parseStringArray(r.getTags());
            List<String> imgs = parseStringArray(r.getImgUrls());
            String cover = imgs.isEmpty() ? null : imgs.getFirst();

            Map<String, Long> counts = counterService.getCounts("post", String.valueOf(r.getId()),
                    List.of("like", "fav"));
            Long likeCount = counts.getOrDefault("like", 0L);
            Long favoriteCount = counts.getOrDefault("fav", 0L);

            Boolean liked = userIdNullable != null
                    && counterService.isLiked("post", String.valueOf(r.getId()), userIdNullable);
            Boolean faved = userIdNullable != null
                    && counterService.isFaved("post", String.valueOf(r.getId()), userIdNullable);
            Boolean isTop = includeIsTop ? r.getIsTop() : null;

            items.add(new FeedItemResponse(
                    String.valueOf(r.getId()),
                    r.getTitle(),
                    r.getDescription(),
                    cover,
                    tags,
                    r.getAuthorAvatar(),
                    r.getAuthorNickname(),
                    r.getAuthorTagJson(),
                    likeCount,
                    favoriteCount,
                    liked,
                    faved,
                    isTop));
        }
        return items;
    }

    private List<String> parseStringArray(String json) {
        if (json == null || json.isBlank())
            return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Writes feed data to Redis and builds reverse index for invalidation.
     */
    private void writeToCache(String pageKey, String idsKey, String hasMoreKey, int size, List<PostFeedRow> rows,
            List<FeedItemResponse> items, boolean hasMore, Duration frTtl) {
        List<String> idVals = new ArrayList<>();

        for (PostFeedRow r : rows) {
            idVals.add(String.valueOf(r.getId()));
        }

        if (!idVals.isEmpty()) {
            redis.opsForList().leftPushAll(idsKey, idVals);
            redis.expire(idsKey, frTtl);
            // Soft limit for hasMore flag
            if (idVals.size() == size && hasMore) {
                redis.opsForValue().set(hasMoreKey, "1",
                        Duration.ofSeconds(10 + ThreadLocalRandom.current().nextInt(11)));
            } else {
                redis.opsForValue().set(hasMoreKey, hasMore ? "1" : "0", Duration.ofSeconds(10));
            }
        }

        // Add this page to the set of pages for public feed, which can be used for
        // invalidation when a post is updated
        redis.opsForSet().add("feed:public:pages", pageKey);

        for (FeedItemResponse it : items) {
            // Reverse index: create a "page reference relationship" for each content by
            // hour.
            long hourSlot = System.currentTimeMillis() / 3600000L;
            String idxKey = "feed:public:index:" + it.id() + ":" + hourSlot;
            redis.opsForSet().add(idxKey, pageKey);
            redis.expire(idxKey, frTtl);

            try {
                String itemKey = "feed:item:" + it.id();
                String itemJson = objectMapper.writeValueAsString(it);
                redis.opsForValue().set(itemKey, itemJson, frTtl);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Construct the cache key for a given page and size.
     * 
     * @param page
     * @param size
     * @return cache key.
     */
    private String cacheKey(int page, int size) {
        return "feed:public:" + size + ":" + page + ":v" + LAYOUT_VERSION;

    }

    private void maybeExtendTtlMine(String key) {
        int baseTtl = 30;
        int target = hotKey.ttlForMine(baseTtl, key);

        Long currentTtl = redis.getExpire(key);
        if (currentTtl != null && currentTtl < target) {
            redis.expire(key, Duration.ofSeconds(target));
        }
    }

}
