package com.star.share.relation.api;

import org.springframework.web.bind.annotation.RestController;

import com.star.share.auth.config.AuthProperties.Jwt;
import com.star.share.auth.token.JwtService;
import com.star.share.counter.service.UserCounterService;
import com.star.share.profile.pojo.ProfileResponse;
import com.star.share.relation.mapper.RelationMapper;
import com.star.share.relation.service.RelationService;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing user relationships such as following and followers.
 * This controller provides endpoints for users to follow/unfollow other users,
 * check following status, and retrieve lists of followers and following users with pagination support.
 * It interacts with the RelationService to perform the necessary business logic and uses JWT for authentication.
 */
@RestController
@RequestMapping("/api/v1/relation")
@RequiredArgsConstructor
public class RelationController {

    private final RelationService relationService;
    private final JwtService jwtService;
    private final StringRedisTemplate redis;
    private final UserCounterService userCounterService;
    private final RelationMapper relationMapper;

    /**
     * Endpoint to follow a user. The authenticated user (extracted from the JWT
     * token)
     * will follow the user specified by toUserId.
     * 
     * @param toUserId the ID of the user to be followed
     * @param jwt      the JWT token containing the authenticated user's information
     * @return true if the follow action was successful, false otherwise
     */
    @PostMapping("/follow")
    public boolean follow(@RequestParam("toUserId") Long toUserId,
            @AuthenticationPrincipal Jwt jwt) {
        long uid = jwtService.extractUserId(null);
        return relationService.follow(uid, toUserId);
    }

    /**
     * Endpoint to unfollow a user. The authenticated user (extracted from the JWT
     * token)
     * will unfollow the user specified by toUserId.
     * 
     * @param toUserId the ID of the user to be unfollowed
     * @param jwt      the JWT token containing the authenticated user's information
     * @return true if the unfollow action was successful, false otherwise
     */
    @PostMapping("/unfollow")
    public boolean unfollow(@RequestParam("toUserId") Long toUserId,
            @AuthenticationPrincipal Jwt jwt) {
        long uid = jwtService.extractUserId(null);
        return relationService.unfollow(uid, toUserId);
    }

    /**
     * Endpoint to check the following status between the authenticated user and
     * another user specified by toUserId.
     * 
     * @param toUserId
     * @param jwt
     * @return
     */
    @GetMapping("/status")
    public Map<String, Boolean> status(@RequestParam("toUserId") long toUserId,
            @AuthenticationPrincipal Jwt jwt) {
        long uid = jwtService.extractUserId(null);
        return relationService.relationsStatus(uid, toUserId);
    }

    @GetMapping("/following")
    public List<ProfileResponse> following(
            @RequestParam("userId") long userId,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "cursor", required = false) Long cursor) {
        int l = Math.min(Math.max(limit, 1), 100);
        return relationService.followingProfiles(userId, l, Math.max(offset, 0), cursor);

    }

    @GetMapping("/followers")
    public List<ProfileResponse> followers(
            @RequestParam("userId") long userId,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "cursor", required = false) Long cursor) {
        int l = Math.min(Math.max(limit, 1), 100);
        return relationService.followersProfiles(userId, l, Math.max(offset, 0), cursor);
    }

    /**
     * Endpoint to retrieve various counters related to a user, such as the number of
     * followings, followers, posts, liked posts, and favorited posts. The method
     * first attempts to retrieve the counters from Redis cache. If the cache is
     * missing or inconsistent with the database, it triggers a consistency check and
     * potentially rebuilds the cache asynchronously.
     * 
     * @param userId the ID of the user for whom to retrieve the counters
     * @return a map containing various counters related to the user
     */
    @GetMapping("/counters")
    public Map<String, Long> counters(@RequestParam("userId") long userId) {
        byte[] raw = redis.execute((RedisCallback<byte[]>) conn -> conn.stringCommands()
                .get(("ucnt:" + userId).getBytes(StandardCharsets.UTF_8)));

        if (raw == null) {
            // TODO: Rebulid the cache if not exist, currently just return 0
        }

        Map<String, Long> map = new LinkedHashMap<>();

        final byte[] buf = raw;
        final int seg = buf.length / 4;

        // Function to read a 32-bit big-endian integer from the byte array at the given
        // index (1-based).
        IntFunction<Long> read = idx -> {
            if (idx < 1 || idx > seg)
                return 0L;
            int off = (idx - 1) * 4;
            long n = 0;
            for (int i = 0; i < 4; i++) {
                n = (n << 8) | (buf[off + i] & 0xFFL);
            }
            return n;
        };

        long sdsFollowings = read.apply(1);
        long sdsFollowers = read.apply(2);

        String chkKey = "ucnt:chk:" + userId;

        // Using Sampled Redis-based locking to ensure that only one request will
        // trigger the consistency check and potential rebuild of the counters for a
        // given userId within a short time window (e.g., 5 minutes). This prevents
        // thundering herd problem in case of cache misses or detected inconsistencies.
        Boolean doCheck = redis.opsForValue().setIfAbsent(chkKey, "1", java.time.Duration.ofSeconds(300));

        if (Boolean.TRUE.equals(doCheck)) {
            int dbFollowings = 0;
            int dbFollowers = 0;

            // Try-catch blocks to handle potential database exceptions gracefully.
            // In case of an exception, the counts will default to 0,
            // which will likely trigger a rebuild if the cache values are inconsistent with
            // the database.
            try {
                dbFollowings = relationMapper.countFollowingActive(userId);
            } catch (Exception ignored) {
            }
            try {
                dbFollowers = relationMapper.countFollowerActive(userId);
            } catch (Exception ignored) {
            }

            if ((seg != 5) || sdsFollowings != (long) dbFollowings || sdsFollowers != (long) dbFollowers) {
                try {
                    // TODO: Rebuild the cache asynchronously to avoid blocking the current request.
                } catch (Exception ignored) {
                }

                byte[] raw2 = redis.execute((RedisCallback<byte[]>) c -> c.stringCommands()
                        .get(("ucnt:" + userId).getBytes(StandardCharsets.UTF_8)));
                if (raw2 != null && raw2.length >= 20) {
                    final byte[] buf2 = raw2;
                    IntFunction<Long> r2 = idx -> {
                        int off = (idx - 1) * 4;
                        long n = 0;
                        for (int i = 0; i < 4; i++) {
                            n = (n << 8) | (buf2[off + i] & 0xFFL);
                        }
                        return n;
                    };
                    map.put("followings", r2.apply(1));
                    map.put("followers", r2.apply(2));
                    map.put("posts", r2.apply(3));
                    map.put("likedPosts", r2.apply(4));
                    map.put("favedPosts", r2.apply(5));
                    return map;
                }
            }
        }

        map.put("followings", sdsFollowings);
        map.put("followers", sdsFollowers);
        map.put("posts", read.apply(3));
        map.put("likedPosts", read.apply(4));
        map.put("favedPosts", read.apply(5));
        return map;

    }

}
