package com.star.share.relation.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.star.share.profile.pojo.ProfileResponse;
import com.star.share.relation.entity.RelationEvent;
import com.star.share.relation.mapper.OutboxMapper;
import com.star.share.relation.mapper.RelationMapper;
import com.star.share.relation.service.RelationService;
import com.star.share.user.entity.User;
import com.star.share.user.mapper.UserMapper;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntFunction;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the RelationService interface for managing user relationships
 * such as following and followers. This class provides methods to follow and
 * unfollow users, check following status, and retrieve lists of followers and
 * following users with pagination support. It also implements caching strategies
 * using Redis and Caffeine to optimize performance for popular users.
 */
@Service
public class RelationServiceImpl implements RelationService {

    private final RelationMapper relationMapper;
    private final OutboxMapper outboxMapper;
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> tokenScript;
    private final ObjectMapper objectMapper;
    private final Cache<Long, List<Long>> flwsTopCache;
    private final Cache<Long, List<Long>> fansTopCache;
    private final UserMapper userMapper;

    private static final String TOKEN_BUCKET_LUA = """

            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local rate = tonumber(ARGV[2])
            local now = redis.call('TIME')[1]
            local last = redis.call('HGET', key, 'last')
            local tokens = redis.call('HGET', key, 'tokens')
            if not last then last = now; tokens = capacity end
            local elapsed = tonumber(now) - tonumber(last)
            local add = elapsed * rate
            tokens = math.min(capacity, tonumber(tokens) + add)
            if tokens < 1 then redis.call('HSET', key, 'last', now); redis.call('HSET', key, 'tokens', tokens); return 0 end
            tokens = tokens - 1
            redis.call('HSET', key, 'last', now)
            redis.call('HSET', key, 'tokens', tokens)
            redis.call('PEXPIRE', key, 60000)
            return 1
            """;

    /**
     * Constructor for RelationServiceImpl.
     * 
     * @param relationMapper the mapper for relation database operations
     * @param outboxMapper   the mapper for outbox database operations
     * @param redis          the Redis template for caching and token management
     * @param objectMapper   the ObjectMapper for JSON processing
     * @param userMapper     the mapper for user database operations
     */
    public RelationServiceImpl(
            RelationMapper relationMapper,
            OutboxMapper outboxMapper,
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            UserMapper userMapper) {

        this.relationMapper = relationMapper;
        this.outboxMapper = outboxMapper;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.userMapper = userMapper;

        this.tokenScript = new DefaultRedisScript<>();
        this.tokenScript.setResultType(Long.class);
        this.tokenScript.setScriptText(TOKEN_BUCKET_LUA);

        this.flwsTopCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();

        this.fansTopCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();
    }

    /**
     * Follow a user.
     * Restrict follow actions using a token bucket algorithm implemented in Lua
     * script.
     * 
     * @param fromUserId the ID of the user who wants to follow
     * @param toUserId   the ID of the user to be followed
     * @return true if the follow action was successful, false otherwise
     * 
     */
    @Override
    @Transactional
    public boolean follow(long fromUserId, long toUserId) {
        // LUA SCRIPT to restrict
        Long ok = redis.execute(
                tokenScript,
                List.of("rl:follow:" + fromUserId),
                "100",
                "1");
        if (ok == null || ok == 0L) {
            return false;
        }

        long id = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
        int inserted = relationMapper.insertFollowing(id, fromUserId, toUserId, 1);

        if (inserted > 0) {
            try {
                Long outId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
                String payload = objectMapper.writeValueAsString(
                        new RelationEvent("FollowCreated", fromUserId, toUserId, id));

                outboxMapper.insert(outId, "following", id, "FollowCreated", payload);

            } catch (Exception e) {
            }
            return true;
        }
        return false;
    }

    /**
     * Unfollow a user, Write an outbox event for the unfollow action.
     * 
     * @param fromUserId the ID of the user who wants to unfollow
     * @param toUserId   the ID of the user to be unfollowed
     * @return true if the unfollow action was successful, false otherwise
     */
    @Override
    @Transactional
    public boolean unfollow(long fromUserId, long toUserId) {
        int updated = relationMapper.cancelFollowing(fromUserId, toUserId);
        if (updated > 0) {
            try {
                Long outId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
                String payload = objectMapper.writeValueAsString(
                        new RelationEvent("FollowCancelled", fromUserId, toUserId, null));
                outboxMapper.insert(outId, "following", outId, "FollowCancelled", payload);
            } catch (Exception e) {
            }
            return true;
        }
        return false;
    }

    /**
     * Check if a user is following another user.
     * 
     * @param fromUserId the ID of the user who may be following
     * @param toUserId   the ID of the user who may be followed
     * @return true if fromUserId is following toUserId, false otherwise
     */
    @Override
    public boolean isFollowing(long fromUserId, long toUserId) {
        return relationMapper.existsFollowing(fromUserId, toUserId) > 0;
    }

    /**
     * Get a paginated list of user IDs that the specified user is following.
     */
    @Override
    public List<Long> following(long userId, int limit, int offset) {
        String key = "uf::flws:" + userId;
        return getListWithOffset(
                key,
                offset,
                limit,
                need -> relationMapper.listFollowerRows(userId, need, 0),
                "fromUserId",
                "createdAt",
                flwsTopCache,
                userId);
    }

    /**
     * Get a paginated list of user IDs that are following the specified user.
     */
    @Override
    public List<Long> followers(long userId, int limit, int offset) {
        String key = "uf:fans:" + userId;
        return getListWithOffset(
                key,
                offset,
                limit,
                need -> relationMapper.listFollowerRows(userId, need, 0),
                "fromUserId",
                "createdAt",
                fansTopCache,
                userId);

    }

    /**
     * Get the relationship status between two users, including whether the first
     * user is following the second,
     * 
     * @param userId      the ID of the first user
     * @param otherUserId the ID of the second user
     * @return a map containing the relationship status with keys "following",
     *         "followedBy
     */
    @Override
    public Map<String, Boolean> relationsStatus(long userId, long otherUserId) {
        boolean following = isFollowing(userId, otherUserId);
        boolean followedBy = isFollowing(otherUserId, userId);
        boolean mutual = following && followedBy;
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put("following", following);
        map.put("followedBy", followedBy);
        map.put("mutual", mutual);
        return map;
    }

    /**
     * Get a paginated list of user IDs that the specified user is following using
     * cursor-based pagination.
     */
    @Override
    public List<Long> followingCursor(long userId, int limit, Long cursor) {
        String key = "uf::flws:" + userId;
        return getListWithCursor(
                key,
                limit,
                cursor,
                need -> relationMapper.listFollowerRows(userId, need, 0),
                "toUserId",
                "createdAt");
    }

    /**
     * Get a paginated list of user IDs that are following the specified user using
     * cursor-based pagination.
     */
    @Override
    public List<Long> followersCursor(long userId, int limit, Long cursor) {
        String key = "uf:fans:" + userId;
        return getListWithCursor(
                key,
                limit,
                cursor,
                need -> relationMapper.listFollowerRows(userId, need, 0),
                "fromUserId",
                "createdAt");
    }

    @Override
    public List<ProfileResponse> followingProfiles(long userId, int limit, int offset, Long cursor) {
        List<Long> ids = cursor == null ? followingCursor(userId, limit, cursor) : following(userId, limit, offset);
        return toProfiles(ids);
    }

    @Override
    public List<ProfileResponse> followersProfiles(long userId, int limit, int offset, Long cursor) {
        List<Long> ids = cursor == null ? followersCursor(userId, limit, cursor) : followers(userId, limit, offset);
        return toProfiles(ids);
    }

    /**
     * Helper method to get a list of IDs with pagination using Redis sorted sets for caching.
     * The method first tries to get the requested range from Redis. If not available,
     * it checks a local Caffeine cache for big V users. If still not found, 
     * it fetches from the database, fills Redis, and then returns the requested range.
     * Using aside-cache pattern to optimize for read performance while ensuring data consistency with the database.
     * 
     * @param key         the Redis key for the sorted set
     * @param offset      the starting index for pagination
     * @param limit       the maximum number of items to return
     * @param rowsFetcher a function that fetches rows from the database given a
     *                    limit, returning a map of ID to row data
     * @param idField     the field name for the ID in the row data
     * @param tsField     the field name for the timestamp in the row data
     * @param localCache  the local cache for big V users
     * @param userId      the ID of the user for whom the list is being fetched
     * @return a list of IDs
     * 
     */
    private List<Long> getListWithOffset(
            String key,
            int offset,
            int limit,
            IntFunction<Map<Long, Map<String, Object>>> rowsFetcher,
            String idField,
            String tsField,
            Cache<Long, List<Long>> localCache,
            long userId) {

        Set<String> cached = redis.opsForZSet().reverseRange(key, offset, offset + limit - 1L);

        if (cached != null && !cached.isEmpty()) {
            return toLongList(cached);
        }

        List<Long> top = localCache != null ? localCache.getIfPresent(userId) : null;
        if (top != null && !top.isEmpty()) {
            int from = Math.min(offset, top.size());
            int to = Math.min(offset + limit, top.size());
            return new ArrayList<>(top.subList(from, to));
        }

        int need = Math.max(1, limit + offset);
        Map<Long, Map<String, Object>> rows = rowsFetcher.apply(Math.min(need, 1000));
        if (rows != null && !rows.isEmpty()) {
            fillZSet(key, rows, idField, tsField, null);
            redis.expire(key, Duration.ofHours(2));

            if (localCache != null && isBigV(userId, 1)) {
                maybeUpdateTopCache(userId, key, localCache);
            }

            Set<String> filled = redis.opsForZSet().reverseRange(key, offset, offset + limit - 1L);
            return filled != null ? toLongList(filled) : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    /**
     * Helper method to get a list of IDs with cursor-based pagination using Redis
     * sorted
     * 
     * @param key         the Redis key for the sorted set
     * @param limit       the maximum number of items to return
     * @param cursor      the cursor for pagination, representing the score
     *                    (timestamp) to start from
     * @param rowsFetcher a function that fetches rows from the database given a
     *                    limit, returning a map of ID to row data
     * @param idField     the field name for the ID in the row data
     * @param tsField     the field name for the timestamp in the row data
     * @return a list of IDs
     */
    private List<Long> getListWithCursor(
            String key,
            int limit,
            Long cursor,
            IntFunction<Map<Long, Map<String, Object>>> rowsFetcher,
            String idField,
            String tsField) {
        double max = cursor == null ? Double.POSITIVE_INFINITY : cursor.doubleValue();
        Set<String> cached = redis.opsForZSet().reverseRangeByScore(key, max, Double.NEGATIVE_INFINITY, 0, limit);
        if (cached != null && !cached.isEmpty()) {
            return toLongList(cached);
        }
        int need = Math.max(limit, 100);
        Map<Long, Map<String, Object>> rows = rowsFetcher.apply(Math.min(need, 1000));
        if (rows != null && !rows.isEmpty()) {
            fillZSet(key, rows, idField, tsField, cursor);
            redis.expire(key, Duration.ofHours(2));
            Set<String> filled = redis.opsForZSet().reverseRangeByScore(key, max, Double.NEGATIVE_INFINITY, 0, limit);
            return filled != null ? toLongList(filled) : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    // Check if the user is a big V (has many followers/following) by looking up a
    // precomputed count in Redis
    private boolean isBigV(long userId, int idx) {
        byte[] raw = redis.execute((RedisCallback<byte[]>) c -> c.stringCommands()
                .get(("ucnt:" + userId).getBytes(StandardCharsets.UTF_8)));
        if (raw == null || raw.length < 20)
            return false;
        long n = 0;
        int off = (idx - 1) * 4;
        for (int i = 0; i < 4; i++)
            n = (n << 8) | (raw[off + i] & 0xFFL);
        return n >= 500_000L;
    }

    // Adding entries to Redis sorted set with timestamp as score for pagination
    private void fillZSet(String key,
            Map<Long, Map<String, Object>> rows,
            String idField,
            String tsField,
            Long cursor) {
        for (Map<String, Object> r : rows.values()) {
            Object idObj = r.get(idField);
            Object tsObj = r.get(tsField);
            if (idObj == null || tsObj == null)
                continue;
            long score = tsScore(tsObj);
            if (cursor == null || score <= cursor) {
                redis.opsForZSet().add(key, String.valueOf(idObj), score);
            }
        }
    }

    // Helper method to convert Set<String> to List<Long>
    private List<Long> toLongList(Set<String> set) {
        List<Long> result = new ArrayList<>(set.size());
        for (String s : set) {
            result.add(Long.valueOf(s));
        }
        return result;
    }

    // Update local TOP cache if the user is a big V (has many followers/following)
    private void maybeUpdateTopCache(long userId, String key, Cache<Long, List<Long>> cache) {
        Set<String> allSet = redis.opsForZSet().reverseRange(key, 0, 499);
        if (allSet == null || allSet.isEmpty())
            return;
        List<Long> all = new ArrayList<>(allSet.size());
        for (String s : allSet)
            all.add(Long.valueOf(s));
        cache.put(userId, all);
    }

    // Extract timestamp in milliseconds from various possible types (Timestamp, Date, etc.) 
    // for use as score in Redis sorted set
    private long tsScore(Object tsObj) {
        if (tsObj instanceof Timestamp ts) {
            return ts.getTime();
        }
        if (tsObj instanceof Date d) {
            return d.getTime();
        }
        return System.currentTimeMillis();
    }

    /**
     * Helper method to convert a list of user IDs to a list of ProfileResponse
     * objects by fetching user details from the database.
     * This method is used to enrich the list of follower/following IDs with user
     * profile information
     * 
     * @param ids the list of user IDs to convert to ProfileResponse objects
     * @return a list of ProfileResponse objects corresponding to the given user IDs
     */
    private List<ProfileResponse> toProfiles(List<Long> ids) {
        if (ids == null || ids.isEmpty())
            return List.of();

        List<User> users = userMapper.listByIds(ids);

        Map<Long, User> map = new LinkedHashMap<>(users.size());
        for (User u : users) {
            map.put(u.getId(), u);
        }

        List<ProfileResponse> out = new ArrayList<>(ids.size());
        for (Long id : ids) {
            User u = map.get(id);
            if (u == null) {
                continue;
            }
            out.add(new ProfileResponse(u.getId(),
                    u.getNickname(),
                    u.getAvatar(),
                    u.getBio(),
                    u.getZgId(),
                    u.getGender(),
                    u.getBirthday(),
                    u.getSchool(),
                    u.getPhone(),
                    u.getEmail(),
                    u.getTagsJson()));

        }
        return out;

    }

}
