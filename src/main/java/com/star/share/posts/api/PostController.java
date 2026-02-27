package com.star.share.posts.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.star.share.auth.token.JwtService;
import com.star.share.posts.entity.dto.PostContentConfirmRequest;
import com.star.share.posts.entity.dto.PostPatchRequest;
import com.star.share.posts.entity.dto.PostVisibilityPatchRequest;
import com.star.share.posts.entity.vo.FeedPageResponse;
import com.star.share.posts.entity.vo.PostDetailResponse;
import com.star.share.posts.entity.vo.PostDraftCreateResponse;
import com.star.share.posts.service.PostFeedService;
import com.star.share.posts.service.PostService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/posts")
@Validated
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostFeedService postFeedService;
    private final JwtService jwtService;

    /**
     * Creates a new post draft.
     *
     * @param jwt authenticated user token
     * @return draft creation response with ID
     */
    @PostMapping("/draft")
    public PostDraftCreateResponse createDraft(@AuthenticationPrincipal Jwt jwt) {

        long userId = jwtService.extractUserId(jwt);
        long draftId = postService.createDraft(userId);
        return new PostDraftCreateResponse(String.valueOf(draftId));
    }

    /**
     * Confirms draft content after upload.
     *
     * @param id post draft ID
     * @param request content confirmation (object key, etag, size, sha256)
     * @param jwt authenticated user token
     * @return 200 OK on success
     */
    @PostMapping("/{id}/content/confirm")
    public ResponseEntity<Void> confirmContent(@PathVariable long id,
            @Valid @RequestBody PostContentConfirmRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);

        postService.confirmContent(userId, id, request.objectKey(), request.etag(),
                request.size(), request.sha256());

        return ResponseEntity.ok().build();
    }

    /**
     * Updates post metadata (title, tags, visibility, etc).
     *
     * @param id post ID
     * @param request metadata fields to update (all optional)
     * @param jwt authenticated user token
     * @return 200 OK on success
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Void> patchMetaData(@PathVariable long id,
            @RequestBody @Valid PostPatchRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);

        postService.updateMetadata(userId, id, request.title(), request.tagId(), request.tags(),
                request.imgUrls(), request.visible(), request.isTop(), request.description());

        return ResponseEntity.ok().build();
    }

    /**
     * Publishes a post draft.
     *
     * @param id post ID
     * @param jwt authenticated user token
     * @return 200 OK on success
     */
    @PatchMapping("/{id}/publish")
    public ResponseEntity<Void> publish(@PathVariable long id,
            @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        postService.publish(userId, id);
        return ResponseEntity.ok().build();
    }

    /**
     * Updates post "top" status (pin/unpin).
     *
     * @param id post ID
     * @param request request containing isTop flag
     * @param jwt authenticated user token
     * @return 200 OK on success
     */
    @PatchMapping("/{id}/top")
    public ResponseEntity<Void> patchTop(@PathVariable long id,
            @RequestBody @Valid PostPatchRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        postService.updateTop(userId, id, request.isTop());
        return ResponseEntity.ok().build();
    }

    /**
     * Updates post visibility.
     *
     * @param id post ID
     * @param request request containing visibility setting
     * @param jwt authenticated user token
     * @return 200 OK on success
     */
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Void> patchVisibility(@PathVariable long id,
            @RequestBody @Valid PostVisibilityPatchRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        postService.updateVisibility(userId, id, request.visible());
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a post.
     *
     * @param id post ID
     * @param jwt authenticated user token
     * @return 200 OK on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id,
            @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        postService.delete(userId, id);
        return ResponseEntity.ok().build();
    }

    /**
     * Gets post details.
     *
     * @param id post ID
     * @param jwt authenticated user token (nullable)
     * @return post detail response
     */
    @GetMapping("/{id}")
    public PostDetailResponse getDetail(@PathVariable long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long currentUserIdNullable = jwt != null ? jwtService.extractUserId(jwt) : null;
        return postService.getDetail(id, currentUserIdNullable);
    }

    /**
     * Retrieves public feed/timeline.
     *
     * @param page page number (default 1)
     * @param size page size (default 10)
     * @param jwt authenticated user token (nullable)
     * @return paginated feed response
     */
    @GetMapping("/feed")
    public FeedPageResponse feed(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt) {
        Long currentUserIdNullable = jwt != null ? jwtService.extractUserId(jwt) : null;
        return postFeedService.getFeed(page, size, currentUserIdNullable);
    }

    /**
     * Retrieves current user's personal posts.
     *
     * @param page page number (default 1)
     * @param size page size (default 10)
     * @param jwt authenticated user token
     * @return paginated feed response
     */
    @GetMapping("/my")
    public FeedPageResponse myFeed(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        return postFeedService.getMyFeed(userId, page, size);
    }
}