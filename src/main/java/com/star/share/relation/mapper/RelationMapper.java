package com.star.share.relation.mapper;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RelationMapper {

    /**
     * INSERT a FOLLOWING relationship into the database.
     * @param id the unique identifier for the relationship
     * @param fromUserId the user ID of the follower
     * @param toUserId the user ID of the followee
     * @param relStatus the status of the relationship
     * @return the number of rows affected by the insert operation
     */
    int insertFollowing(@Param("id") Long id,
                        @Param("fromUserId") Long fromUserId,
                        @Param("toUserId") Long toUserId,
                        @Param("relStatus") Integer relStatus);

    /**
     * DELETE a FOLLOWING relationship from the database.
     * @param fromUserId the user ID of the follower
     * @param toUserId the user ID of the followee
     * @return the number of rows affected by the delete operation
     */
    int cancelFollowing(@Param("fromUserId") Long fromUserId,
                        @Param("toUserId") Long toUserId);

    /**
     * INSERT a FOLLOWER relationship into the database.
     * @param id the unique identifier for the relationship
     * @param toUserId the user ID of the followee
     * @param fromUserId the user ID of the follower
     * @param relStatus the status of the relationship
     * @return the number of rows affected by the insert operation
     */
    int insertFollower(@Param("id") Long id,
                       @Param("toUserId") Long toUserId,
                       @Param("fromUserId") Long fromUserId,
                       @Param("relStatus") Integer relStatus);

    /**
     * DELETE a FOLLOWER relationship from the database.
     * @param toUserId the user ID of the followee
     * @param fromUserId the user ID of the follower
     * @return the number of rows affected by the delete operation
     */
    int cancelFollower(@Param("toUserId") Long toUserId,
                       @Param("fromUserId") Long fromUserId);、

    /**
     * CHECK if a FOLLOWING relationship exists between two users.
     * @param fromUserId the user ID of the follower
     * @param toUserId the user ID of the followee
     * @return 1 if the relationship exists, 0 otherwise
     */
    int existsFollowing(@Param("fromUserId") Long fromUserId,
                        @Param("toUserId") Long toUserId);

    /**
     * CHECK if a FOLLOWER relationship exists between two users.
     * @param fromUserId the user ID of the follower
     * @param limit the maximum number of results to return
     * @param offset the number of results to skip before starting to return results
     * @return a list of user IDs that the specified user is following, limited by the provided parameters
     */
    List<Long> listFollowing(@Param("fromUserId") Long fromUserId,
                             @Param("limit") int limit,
                             @Param("offset") int offset);

    /**
     * CHECK if a FOLLOWER relationship exists between two users.
     * @param toUserId the user ID of the followee
     * @param limit the maximum number of results to return
     * @param offset the number of results to skip before starting to return results
     * @return a list of user IDs that are following the specified user, limited by the provided parameters
     */
    List<Long> listFollowers(@Param("toUserId") Long toUserId,
                             @Param("limit") int limit,
                             @Param("offset") int offset);

    /**
     * CHECK if a FOLLOWING relationship exists between two users.
     * @param toUserId the user ID of the followee
     * @param limit the maximum number of results to return
     * @param offset the number of results to skip before starting to return results
     * @return a map where the key is the user ID of the followee and the value is another map containing details about the follower relationship, limited by the provided parameters
     */
    @MapKey("toUserId")
    Map<Long, Map<String, Object>> listFollowerRows(@Param("toUserId") Long toUserId,
                                                    @Param("limit") int limit,
                                                    @Param("offset") int offset);

    /**
     * COUNT the number of active FOLLOWING relationships for a given user.
     * @param fromUserId the user ID of the follower
     * @return the number of active FOLLOWING relationships for the specified user
     */
    int countFollowingActive(@Param("fromUserId") Long fromUserId);

    /**
     * COUNT the number of active FOLLOWER relationships for a given user.
     * @param toUserId the user ID of the followee
     * @return the number of active FOLLOWER relationships for the specified user
     */
    int countFollowerActive(@Param("toUserId") Long toUserId);
}

