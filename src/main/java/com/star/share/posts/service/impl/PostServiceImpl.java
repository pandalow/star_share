package com.star.share.posts.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.star.share.cache.hotkey.HotKeyDetector;
import com.star.share.common.exception.BusinessException;
import com.star.share.common.exception.ErrorCode;
import com.star.share.counter.service.CounterService;
import com.star.share.counter.service.UserCounterService;
import com.star.share.oss.config.OSSProperties;
import com.star.share.posts.dao.PostMapper;
import com.star.share.posts.entity.model.Post;
import com.star.share.posts.entity.vo.PostDetailResponse;
import com.star.share.posts.id.SnowflakeIdGenerator;
import com.star.share.posts.service.FeedCacheService;
import com.star.share.posts.service.PostService;
import com.star.share.posts.entity.model.PostDetailRow;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final SnowflakeIdGenerator idGenerator;
    private final PostMapper postMapper;
    private final FeedCacheService feedCacheService;
    private final StringRedisTemplate redis;
    private final static int DETAIL_LAYOUT_VER = 1;
    private final UserCounterService userCounterService;
    private final ObjectMapper objectMapper;
    private final OSSProperties ossProperties;
    private final CounterService counterService;
    private final ConcurrentHashMap<String, Object> singleFlight = new ConcurrentHashMap<>();
    private final HotKeyDetector hotKey;

    /**
     * Create a new post draft for the specified creator. The draft will be
     * initialized with default values and a unique ID.
     * 
     * @param creatorId the ID of the user creating the draft
     * @return the ID of the newly created draft
     */
    @Override
    @Transactional
    public long createDraft(long creatorId) {
        long id = idGenerator.nextId();
        Instant now = Instant.now();
        Post post = Post.builder()
                .id(id)
                .creatorId(creatorId)
                .status("draft")
                .type("image_text")
                .visible("public")
                .createTime(now)
                .updateTime(now)
                .build();

        postMapper.insertDraft(post);
        return id;
    }

    /**
     * Confirm the content of a post draft after the client has uploaded the
     * content. This method will validate the provided content details (object key,
     * etag, size, sha256) and update the post draft accordingly. Only the creator
     * of the post can confirm its content.
     * 
     * @param creatorId the ID of the user confirming the content, used for
     *                  authorization
     * @param id        the ID of the post draft to confirm
     * @param objectKey the object key of the uploaded content, used to link the
     *                  content to the post draft
     * @param etag      the ETag of the uploaded content, used for validating the
     *                  integrity of the content
     * @param size      the size of the uploaded content, used for validating the
     *                  content against expected size
     * @param sha256    the SHA-256 hash of the uploaded content, used for
     *                  validating the integrity of the content against expected
     *                  hash
     */
    @Override
    public void confirmContent(long creatorId, long id, String objectKey, String etag, Long size, String sha256) {
        feedCacheService.deleteAllCache();
        feedCacheService.deleteMyFeedCache(creatorId);
        redis.delete("post:detail" + id + ":v" + DETAIL_LAYOUT_VER);

        Post post = Post.builder()
                .id(id)
                .creatorId(creatorId)
                .contentObjectKey(objectKey)
                .contentEtag(etag)
                .contentSize(size)
                .contentSha256(sha256)
                .contentUrl(publicUrl(objectKey))
                .createTime(Instant.now())
                .build();

        int updated = postMapper.updateContent(post);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Post not found or user is not the creator");
        }

        feedCacheService.doubleDeleteCache(200);
        feedCacheService.doubleDeleteMyFeedCache(id, 200);
        redis.delete("post:detail" + id + ":v" + DETAIL_LAYOUT_VER);

        // TODO: adding ragindex update logic here
    }

    /**
     * Update the metadata of an existing post
     * e.g., title, tags, visibility, etc. Only the creator of the post can update
     * its metadata. This method allows for partial updates, meaning that only the
     * fields provided in the request will be updated, while other fields will
     * remain unchanged.
     */
    @Override
    @Transactional
    public void updateMetadata(long creatorId, long id, String title, Long tagId, List<String> tags,
            List<String> imgUrls, String visible, Boolean isTop, String description) {
        feedCacheService.deleteAllCache();
        feedCacheService.deleteMyFeedCache(creatorId);
        redis.delete("post:detail" + id + ":v" + DETAIL_LAYOUT_VER);
        Post post = Post.builder()
                .id(id)
                .creatorId(creatorId)
                .title(title)
                .tagId(tagId)
                .imgUrls(toJsonOrNull(imgUrls))
                .visible(visible)
                .isTop(isTop)
                .description(description)
                .type("image_text")
                .updateTime(Instant.now())
                .build();

        int updated = postMapper.updateMetadata(post);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Post not found or user is not the creator");
        }
        feedCacheService.doubleDeleteCache(200);
        feedCacheService.doubleDeleteMyFeedCache(id, 200);
        redis.delete("post:detail" + id + ":v" + DETAIL_LAYOUT_VER);

        // TODO: adding ragindex update logic here

    }

    @Override
    @Transactional
    public void publish(long creatorId, long id) {
        feedCacheService.deleteAllCache();
        feedCacheService.deleteMyFeedCache(creatorId);
        redis.delete("post:detail" + id + ":v" + DETAIL_LAYOUT_VER);
        int updated = postMapper.publish(id, creatorId);

        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Post not found or user is not the creator");
        }
        try {
            // TODO: finish the update of user post count, currently we can just ignore the
            // failure of this operation to avoid affecting the main flow of publishing
            // posts
            userCounterService.incrementPosts(creatorId, 1);
        } catch (Exception e) {
            // Log the exception or handle it as needed
        }
        feedCacheService.doubleDeleteCache(200);
        feedCacheService.doubleDeleteMyFeedCache(id, 200);
        redis.delete("post:detail" + id + ":v" + DETAIL_LAYOUT_VER);

        // TODO: adding ragindex update logic here
    }

    /**
     * Setting or unsetting a post as top (pinned) will affect its ordering in the
     * feed.
     */
    @Override
    @Transactional
    public void updateTop(long creatorId, long id, boolean isTop) {
        feedCacheService.deleteAllCache();
        feedCacheService.deleteMyFeedCache(creatorId);
        redis.delete("post:detail" + id + ":v" + DETAIL_LAYOUT_VER);
        int updated = postMapper.updateTop(id, creatorId, isTop);

        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Post not found or user is not the creator");
        }
        feedCacheService.doubleDeleteCache(200);
        feedCacheService.doubleDeleteMyFeedCache(id, 200);
        redis.delete("post:detail" + id + ":v" + DETAIL_LAYOUT_VER);
    }

    /**
     * Updating the visibility of a post (e.g., public, private, friends-only) will
     * affect who can see the post in the feed and its ordering. Only the creator of
     * the post can update its visibility.
     */
    @Override
    @Transactional
    public void updateVisibility(long creatorId, long id, String visible) {
        feedCacheService.deleteAllCache();
        feedCacheService.deleteMyFeedCache(creatorId);
        redis.delete("post:detail" + id + ":v" + DETAIL_LAYOUT_VER);
        int updated = postMapper.updateVisibility(id, creatorId, visible);

        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Post not found or user is not the creator");
        }
        feedCacheService.doubleDeleteCache(200);
        feedCacheService.doubleDeleteMyFeedCache(id, 200);
        redis.delete("post:detail" + id + ":v" + DETAIL_LAYOUT_VER);
    }

    /**
     * Remove it from the feed and make it inaccessible to users.
     */
    @Override
    public void delete(long creatorId, long id) {
        feedCacheService.deleteAllCache();
        feedCacheService.deleteMyFeedCache(creatorId);
        redis.delete("post:detail" + id + ":v" + DETAIL_LAYOUT_VER);
        int updated = postMapper.softDelete(id, creatorId);

        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Post not found or user is not the creator");
        }
        feedCacheService.doubleDeleteCache(200);
        feedCacheService.doubleDeleteMyFeedCache(id, 200);
        redis.delete("post:detail" + id + ":v" + DETAIL_LAYOUT_VER);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDetailResponse getDetail(long id, Long currentUserIdNullable) {
        String pageKey = "post:detail" + id + ":v" + DETAIL_LAYOUT_VER;
        String cached = redis.opsForValue().get(pageKey);
        if (cached != null) {
            if ("NULL".equals(cached)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "The post is not existed");
            }

            try {

                PostDetailResponse base = objectMapper.readValue(cached, PostDetailResponse.class);

                hotKey.record(pageKey);
                maybeExtendTtlDetail(pageKey);

                String cntKey = "feed:count:" + id;
                String cntJson = redis.opsForValue().get(cntKey);
                Long likeCount = base.likeCount();
                Long favoriteCount = base.favoriteCount();

                if (cntJson != null) {
                    try {
                        Map<String, Long> cm = objectMapper.readValue(cntJson, new TypeReference<Map<String, Long>>() {
                        });
                        likeCount = cm.getOrDefault("like", likeCount == null ? 0L : likeCount);
                        favoriteCount = cm.getOrDefault("favorite", favoriteCount == null ? 0L : favoriteCount);
                    } catch (Exception e) {
                        // If parsing fails, we can ignore the error and use the counts from the base
                        // response
                    }
                }

                boolean liked = currentUserIdNullable != null
                        && counterService.isLiked("post", String.valueOf(id), currentUserIdNullable);
                boolean favorited = currentUserIdNullable != null
                        && counterService.isFaved("post", String.valueOf(id), currentUserIdNullable);

                return new PostDetailResponse(
                        String.valueOf(id),
                        base.title(),
                        base.description(),
                        base.contentUrl(),
                        base.images(),
                        base.tags(),
                        base.authorId(),
                        base.authorAvatar(),
                        base.authorNickname(),
                        base.authorTagJson(),
                        likeCount,
                        favoriteCount,
                        liked,
                        favorited,
                        base.isTop(),
                        base.visible(),
                        base.type(),
                        base.publishTime());
            } catch (Exception e) {
            }
        }

        Object lock = singleFlight.computeIfAbsent(pageKey, k -> new Object());

        // Using try-finally to ensure that the singleFlight entry is removed after
        // processing, preventing memory leaks
        try {
            synchronized (lock) {
                String again = redis.opsForValue().get(pageKey);
                if (again != null && !"NULL".equals(again)) {
                    try {
                        PostDetailResponse base = objectMapper.readValue(again, PostDetailResponse.class);
                        hotKey.record(pageKey);
                        maybeExtendTtlDetail(pageKey);

                        String cntKey = "feed:count:" + id;
                        String cntJson = redis.opsForValue().get(cntKey);
                        Long likeCount = base.likeCount();
                        Long favoriteCount = base.favoriteCount();

                        if (cntKey != null) {
                            try {
                                Map<String, Long> cm = objectMapper.readValue(cntJson,
                                        new TypeReference<Map<String, Long>>() {
                                        });
                                likeCount = cm.getOrDefault("like", likeCount == null ? 0L : likeCount);
                                favoriteCount = cm.getOrDefault("favorite", favoriteCount == null ? 0L : favoriteCount);
                            } catch (Exception e) {
                            }
                        }

                        boolean liked = currentUserIdNullable != null
                                && counterService.isLiked("post", String.valueOf(id), currentUserIdNullable);
                        boolean favorited = currentUserIdNullable != null
                                && counterService.isFaved("post", String.valueOf(id), currentUserIdNullable);

                        return new PostDetailResponse(
                                String.valueOf(id),
                                base.title(),
                                base.description(),
                                base.contentUrl(),
                                base.images(),
                                base.tags(),
                                base.authorId(),
                                base.authorAvatar(),
                                base.authorNickname(),
                                base.authorTagJson(),
                                likeCount,
                                favoriteCount,
                                liked,
                                favorited,
                                base.isTop(),
                                base.visible(),
                                base.type(),
                                base.publishTime());
                    } catch (Exception e) {
                    } finally {
                        singleFlight.remove(pageKey);
                    }
                }

                PostDetailRow row = postMapper.findDetailById(id);
                if (row == null || "deleted".equals(row.getStatus())) {
                    redis.opsForValue().set(pageKey, "NULL",
                            Duration.ofSeconds(30 + ThreadLocalRandom.current().nextInt(31)));
                    singleFlight.remove(pageKey);
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "The post is not existed");
                }
                boolean isPublic = "published".equals(row.getStatus()) && "public".equals(row.getVisible());
                boolean isOwner = currentUserIdNullable != null && row.getCreatorId() != null
                        && currentUserIdNullable.equals(row.getCreatorId());

                if (!isPublic && !isOwner) {
                    singleFlight.remove(pageKey);
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "The post is not authorized to access");
                }

                List<String> images = parseStringArray(row.getImgUrls());
                List<String> tags = parseStringArray(row.getTags());
                Map<String, Long> counts = counterService.getCounts("knowpost", String.valueOf(row.getId()),
                        List.of("like", "fav"));
                Long favoriteCount = counts.getOrDefault("fav", 0L);
                Long likeCount = counts.getOrDefault("like", 0L);

                PostDetailResponse detail = new PostDetailResponse(
                        String.valueOf(row.getId()),
                        row.getTitle(),
                        row.getDescription(),
                        row.getContentUrl(),
                        images,
                        tags,
                        String.valueOf(row.getCreatorId()),
                        row.getAuthorAvatar(),
                        row.getAuthorNickname(),
                        row.getAuthorTagJson(),
                        likeCount,
                        favoriteCount,
                        null,
                        null,
                        row.getIsTop(),
                        row.getVisible(),
                        row.getType(),
                        row.getPublishTime());

                try {
                    String json = objectMapper.writeValueAsString(detail);
                    int baseTtl = 60;
                    int jitter = ThreadLocalRandom.current().nextInt(30);
                    int target = hotKey.ttlForPublic(baseTtl, pageKey);
                    redis.opsForValue().set(pageKey, json, Duration.ofSeconds(Math.max(target, baseTtl + jitter)));

                } catch (Exception e) {
                }

                boolean liked = currentUserIdNullable != null
                        && counterService.isLiked("post", String.valueOf(id), currentUserIdNullable);
                boolean favorited = currentUserIdNullable != null
                        && counterService.isFaved("post", String.valueOf(id), currentUserIdNullable);
                singleFlight.remove(pageKey);

                return new PostDetailResponse(
                        String.valueOf(row.getId()),
                        detail.title(),
                        detail.description(),
                        detail.contentUrl(),
                        detail.images(),
                        detail.tags(),
                        detail.authorId(),
                        detail.authorAvatar(),
                        detail.authorNickname(),
                        detail.authorTagJson(),
                        detail.likeCount(),
                        detail.favoriteCount(),
                        liked,
                        favorited,
                        detail.isTop(),
                        detail.visible(),
                        detail.type(),
                        detail.publishTime());
            }

        } finally {
            singleFlight.remove(pageKey);
        }

    }

    // Helper method to generate a public URL for content based on the object key

    private void maybeExtendTtlDetail(String pageKey) {
        int baseTtl = 60;
        int target = hotKey.ttlForPublic(baseTtl, pageKey);
        Long currentTtl = redis.getExpire(pageKey);
        if (currentTtl < target) {
            redis.expire(pageKey, Duration.ofSeconds(target));
        }
    }

    private List<String> parseStringArray(String json) {
        if (json == null || json.isBlank())
            return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String toJsonOrNull(List<String> list) {
        if (list == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to serialize list to JSON");
        }
    }

    private String publicUrl(String objectKey) {
        String publicDomain = ossProperties.getPublicDomain();

        if (publicDomain != null && !publicDomain.isEmpty()) {
            return publicDomain.replaceAll("/$", "") + "/" + objectKey;
        } else {
            // Fallback to default URL construction if public domain is not configured
            return "https://" + ossProperties.getBucket() + "." + ossProperties.getEndpoint() + "/" + objectKey;
        }

    }

}
