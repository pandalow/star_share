package com.star.share.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.star.share.user.entity.User;

import java.util.List;

@Mapper
public interface UserMapper {
    User findByPhone(@Param("phone") String phone);
    User findByEmail(@Param("email") String email);
    User findById(@Param("id") Long id);
    boolean existByPhone(@Param("phone") String phone);
    boolean existByEmail(@Param("email") String email);
    void insert(User user);
    void updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);
    void updateProfile(User user);
    boolean existByZgIdExpectId(@Param("zgId") String zgId, @Param("excludeId") Long excludeId);
    List<User> listByIds(@Param("ids") List<Long> ids);
}
