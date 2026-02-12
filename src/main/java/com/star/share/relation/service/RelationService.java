package com.star.share.relation.service;

import java.util.List;
import java.util.Map;

public interface RelationService {

    boolean follow(long fromUserId, long toUserId);

    boolean unfollow(long fromUserId, long toUserId);

    boolean isFollowing(long fromUserId, long toUserId);

    List<Long> following(long userId, int limit, int offset);

    List<Long> followers(long userId, int limit, int offset);

    Map<String, Boolean> relationsStatus(long userId, long otherUserId);

    List<Long> followingCursor(long userId, int limit, Long cursor);

    List<ProfileResponse> followingProfiles(long user, int limit, int offset, Long cursor);
    

}
