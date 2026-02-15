package com.star.share.relation.service;

import java.util.List;
import java.util.Map;

import com.star.share.profile.pojo.ProfileResponse;

import co.elastic.clients.elasticsearch.core.search.Profile;

/**
 * Relation Service interface for managing user relationships 
 * such as following and followers. 
 */
public interface RelationService {

    /**
     * Follow a user.
     * @param fromUserId the ID of the user who wants to follow
     * @param toUserId the ID of the user to be followed
     * @return true if the follow action was successful, false otherwise
     */
    boolean follow(long fromUserId, long toUserId);

    /**
     * Unfollow a user. 
     * @param fromUserId the ID of the user who wants to unfollow
     * @param toUserId the ID of the user to be unfollowed
     * @return true if the unfollow action was successful, false otherwise 
     */
    boolean unfollow(long fromUserId, long toUserId);

    boolean isFollowing(long fromUserId, long toUserId);

    List<Long> following(long userId, int limit, int offset);

    List<Long> followers(long userId, int limit, int offset);

    Map<String, Boolean> relationsStatus(long userId, long otherUserId);

    List<Long> followingCursor(long userId, int limit, Long cursor);
    List<Long> followersCursor(long userId, int limit, Long cursor);
    List<ProfileResponse> followingProfiles(long userId, int limit, int offset, Long cursor);
    List<ProfileResponse> followersProfiles(long userId, int limit, int offset, Long cursor);

}
