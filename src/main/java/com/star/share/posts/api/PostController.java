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
     * Create a new post draft for the authenticated user. The draft will be
     * initialized with default values and a unique ID.
     * 
     * @param jwt The JWT token of the authenticated user.
     * @return The response containing the ID of the newly created draft.
     */
    @PostMapping("/draft")
    public PostDraftCreateResponse createDraft(@AuthenticationPrincipal Jwt jwt) {

        long userId = jwtService.extractUserId(jwt);
        postService.createDraft(userId);
        return new PostDraftCreateResponse(String.valueOf(userId));
    }

    /**
     * Confirm the content of a post draft after the client has uploaded the content
     * 
     * @param id      the ID of the post draft to confirm
     * @param request the request body containing the content confirmation details
     *                (object key, etag, size, sha256)
     * @param jwt     the JWT token of the authenticated user, used to extract the
     *                user ID for authorization and processing
     * @return a ResponseEntity with HTTP status 200 OK if the content confirmation
     *         is successful, or an appropriate error status if it fails (e.g., 400
     *         Bad Request for invalid input, 403 Forbidden for unauthorized access)
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
     * Patch update the metadata of an existing post, such as title, tags,
     * visibility, etc.
     * Only the fields provided in the request will be updated, allowing for partial
     * updates.
     * 
     * @param id      the ID of the post to update
     * @param request the request body containing the metadata fields to update, all
     *                fields are optional
     * @param jwt     the JWT token of the authenticated user, used to extract the
     *                user ID for authorization and processing
     * @return a ResponseEntity with HTTP status 200 OK if the update is successful.
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
     * Publish a post draft, making it visible to the public or according to its
     * visibility settings. Only the creator of the post can publish it.
     * 
     * @param id
     * @param jwt
     * @return a ResponseEntity with HTTP status 200 OK if the publish action is
     *         successful, or an appropriate error status if it fails (e.g., 403
     *         Forbidden for unauthorized access, 404 Not Found if the post does not
     *         exist)
     */
    @PatchMapping("/{id}/publish")
    public ResponseEntity<Void> publish(@PathVariable long id,
            @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        postService.publish(userId, id);
        return ResponseEntity.ok().build();
    }

    /**
     * Update the "top" status of a post, which determines whether the post is
     * pinned to the top of the feed. Only the creator of the post can update this
     * status.
     * 
     * @param id      the ID of the post to update
     * @param request the request body containing the new "top" status (true for
     *                pinned, false for unpinned)
     * @param jwt     the JWT token of the authenticated user, used to extract the
     *                user ID for authorization and processing
     * @return a ResponseEntity with HTTP status 200 OK if the update is successful,
     *         or an
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
     * Update the visibility of a post, which determines who can see the post. Only
     * the creator of the post can update its visibility.
     * 
     * @param id      the ID of the post to update
     * @param request the request body containing the new visibility setting (e.g.,
     *                "public", "private", "friends")
     * @param jwt     the JWT token of the authenticated user, used to extract the
     *                user ID for authorization and processing
     * @return a ResponseEntity with HTTP status 200 OK if the update is successful,
     *         or an appropriate error status if it fails (e.g., 403 Forbidden for
     *         unauthorized access, 404 Not Found if the post does not exist)
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
     * Delete a post, removing it from the feed and making it inaccessible to users.
     * Only
     * the creator of the post can delete it.
     * 
     * @param id  the ID of the post to delete
     * @param jwt the JWT token of the authenticated user, used to extract the user
     *            ID for authorization and processing
     * @return a ResponseEntity with HTTP status 200 OK if the deletion is
     *         successful, or an appropriate error status if it fails (e.g., 403
     *         Forbidden for unauthorized access, 404 Not Found if the post does not
     *         exist)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id,
            @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        postService.delete(userId, id);
        return ResponseEntity.ok().build();
    }

    /**
     * Get the detailed information of a post, including its content, metadata, and
     * creator information. The visibility of the post will be determined based on
     * the post's settings and the relationship between the current user and the creator.
     * @param id
     * @param jwt
     * @return
     */
    @GetMapping("/{id}")
    public PostDetailResponse getDetail(@PathVariable long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long currentUserIdNullable = jwt != null ? jwtService.extractUserId(jwt) : null;
        return postService.getDetail(id, currentUserIdNullable);
    }

    /**
     * Get a paginated list of posts for the feed, which may include posts from followed users, recommended posts, or posts based on other criteria. The visibility of each post in the feed will be determined based on the post's settings and the relationship between the current user and the creator.
     * @param page the page number to retrieve, starting from 1
     * @param size the number of posts per page
     * @param jwt the JWT token of the authenticated user, used to extract the user ID for authorization and processing
     * @return a FeedPageResponse containing the paginated list of posts and related metadata
     */
    @GetMapping("/feed")
    public FeedPageResponse feed(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "10") int size,
        @AuthenticationPrincipal Jwt jwt) {
        Long currentUserIdNullable = jwt != null ? jwtService.extractUserId(jwt) : null;
        return postFeedService.getFeed(page, size, currentUserIdNullable);
    }
}