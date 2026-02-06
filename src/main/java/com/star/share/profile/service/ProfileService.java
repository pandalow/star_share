package com.star.share.profile.service;

import com.star.share.profile.pojo.ProfilePatchRequest;
import com.star.share.profile.pojo.ProfileResponse;
import com.star.share.user.entity.User;

import java.util.Optional;

public interface ProfileService {

    Optional<User> getById(long userId);
    ProfileResponse updateProfile(long userId, ProfilePatchRequest request);

    ProfileResponse updateAvatar(long userId, String url);
}
