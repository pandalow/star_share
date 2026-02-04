package com.star.share.user.service;

import com.star.share.user.entity.User;

import java.util.Optional;

public interface UserService {
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    User createUser(User user);
    void updatePassword(User user);
}
