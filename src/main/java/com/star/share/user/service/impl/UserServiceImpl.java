package com.star.share.user.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.star.share.user.entity.User;
import com.star.share.user.service.UserService;
import com.star.share.user.mapper.UserMapper;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    /**
     * Find User by phone
     * @param phone phone number
     * @return User (Optional)
     */
    @Transactional(readOnly = true)
    public Optional<User> findByPhone(String phone){
        return Optional.ofNullable(userMapper.findByPhone(phone));
    }

    /**
     * Find User by email
     * @param email
     * @return User(Optional)
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email){
        return Optional.ofNullable(userMapper.findByEmail(email));
    }

    /**
     * Find User by Unique ID
     * @param id
     * @return User(Optional)
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id){
        return Optional.ofNullable(userMapper.findById(id));
    }

    /**
     * Check phone number exists
     * @param phone phone number
     * @return boolean
     */
    @Transactional(readOnly = true)
    public boolean existsByPhone(String phone){
        return userMapper.existByPhone(phone);
    }


    /**
     * Checking email exists
     *
     * @param email email address
     * @return boolean
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userMapper.existByEmail(email);
    }

    /**
     * Create User, Write Update time and persistent
     * @param user User entity
     * @return Persistent Entity of User
     */
    @Transactional
    public User createUser(User user){
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return user;
    }

    /**
     * Update user password(Hash), Write update time
     * @param user User Entity(must have Id and new passwordHash)
     */
    @Transactional
    public void updatePassword(User user) {
        user.setUpdatedAt(Instant.now());
        userMapper.updatePassword(user.getId(), user.getPasswordHash());
    }
}
