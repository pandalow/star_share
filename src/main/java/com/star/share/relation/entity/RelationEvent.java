package com.star.share.relation.entity;

/**
 * Represents an event related to user relations, such as following or unfollowing another user.
 * @param type the type of relation event (e.g., "follow", "unfollow")
 * @param fromUserId the ID of the user who initiated the relation event (e.g., the follower)
 * @param toUserId the ID of the user who is the target of the relation event (e.g., the followee)
 * @param id  the unique ID of the relation event, typically corresponding to the database record ID for this event
 */
public record RelationEvent(
        String type,
        Long fromUserId,
        Long toUserId,
        Long id
) {


}
