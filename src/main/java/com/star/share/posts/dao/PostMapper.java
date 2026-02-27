package com.star.share.posts.dao;

import com.star.share.posts.entity.model.Post;
import com.star.share.posts.entity.model.PostFeedRow;
import com.star.share.posts.entity.model.PostDetailRow;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMapper {
    void insertDraft(Post post);

    Post findById(@Param("id") Long id);

    int updateContent(Post post);

    int updateMetadata(Post post);

    int publish(@Param("id") Long id, @Param("creatorId") Long creatorId);

    // Home feed list (public posts from all users), with pinned posts first, then sorted by publish time desc.
    List<PostFeedRow> listFeedPublic(@Param("limit") int limit,
                                     @Param("offset") int offset);

    // My posts list (only my published posts), with pinned posts first, then sorted by publish time desc.
    List<PostFeedRow> listMyPublished(@Param("creatorId") long creatorId,
                                          @Param("limit") int limit,
                                          @Param("offset") int offset);

    // Set top (pin/unpin)
    int updateTop(@Param("id") Long id, @Param("creatorId") Long creatorId, @Param("isTop") Boolean isTop);

    // Set visibility (public/private)
    int updateVisibility(@Param("id") Long id, @Param("creatorId") Long creatorId, @Param("visible") String visible);

    // Soft delete (mark as deleted without removing from database)
    int softDelete(@Param("id") Long id, @Param("creatorId") Long creatorId);

    // Detail
    PostDetailRow findDetailById(@Param("id") Long id);

    long countMyPublished(@Param("creatorId") long creatorId);

    List<Long> listMyPublishedIds(@Param("creatorId") long creatorId);
}
