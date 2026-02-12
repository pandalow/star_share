package com.star.share.profile.service;

import com.star.share.common.exception.BusinessException;
import com.star.share.common.exception.ErrorCode;
import com.star.share.profile.pojo.ProfilePatchRequest;
import com.star.share.profile.pojo.ProfileResponse;
import com.star.share.user.entity.User;
import com.star.share.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * ProfileServiceImpl is the implementation of the ProfileService interface, responsible for handling user profile
 * related operations such as retrieving and updating user profile information.
 */
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserMapper userMapper;

    /**
     * Get User info by user ID
     *
     * @param userId User ID
     * @return Optional of User, empty if not found
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> getById(long userId) {
        return Optional.ofNullable(userMapper.findById(userId));
    }

    /**
     * Update user profile information. This method performs a partial update, meaning that only the fields provided in the request will be updated,
     * and the rest will remain unchanged.
     *
     * @param userId  The ID of the user whose profile is being updated
     * @param request The profile patch request containing the fields to update. All fields are optional,
     *                but at least one must be provided for the update to proceed
     */
    @Override
    public ProfileResponse updateProfile(long userId, ProfilePatchRequest request) {
        // Read the current user info from the database
        User current = userMapper.findById(userId);
        if (current == null) {
            throw new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND, "User not found");
        }

        // Check if there are any fields to update. If all fields in the request are null,
        // it means the client did not provide any data to update, which is a bad request.
        boolean hasFieldToUpdate = request.nickname() != null ||
                request.bio() != null ||
                request.birthday() != null ||
                request.gender() != null ||
                request.school() != null ||
                request.zgId() != null ||
                request.tagJson() != null;

        if (!hasFieldToUpdate) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "No fields to update");
        }

        if (request.zgId() != null && !request.zgId().isBlank()) {
            boolean exists = userMapper.existsByZgIdExceptId(
                    request.zgId(),
                    current.getId()
            );

            if (exists) {
                throw new BusinessException(ErrorCode.ZGID_EXISTS);
            }
        }

        User patch = updateUser(request, current);
        userMapper.updateProfile(patch);

        User user = userMapper.findById(userId);

        return convertToProfileResponse(user);
    }

    /**
     * Update user avatar URL. This method updates the avatar URL of the user
     * in the database and returns the updated profile information.
     * @param userId  The ID of the user whose avatar is being updated
     * @param url The new avatar URL to be set for the user
     * @return A ProfileResponse object containing the updated profile information
     *          after the avatar URL has been updated
     */
    @Override
    public ProfileResponse updateAvatar(long userId, String url) {
        User current = userMapper.findById(userId);
        if (current == null) {
            throw new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND, "User not found");
        }

        User patch = new User();
        patch.setId(current.getId());
        patch.setAvatar(url);
        userMapper.updateProfile(patch);

        return convertToProfileResponse(
                userMapper.findById(userId)
        );
    }

    /**
     * Create a new User object with only the fields that need to be updated.
     * This way, we can perform a partial update in the database, only changing the fields that are not null in the patch object.
     *
     * @param request The profile patch request containing the fields to update
     * @param current The current User object from the database, used to get the user ID and to check which fields need to be updated
     * @return A new User object with only the fields to update, and the ID set to the current user's ID
     */
    private static User updateUser(ProfilePatchRequest request, User current) {
        User patch = new User();
        patch.setId(current.getId());

        if (request.nickname() != null) {
            patch.setNickname(request.nickname().trim());
        }
        if (request.bio() != null) {
            patch.setBio(request.bio().trim());
        }
        if (request.gender() != null) {
            patch.setGender(request.gender().trim().toUpperCase());
        }
        if (request.birthday() != null) {
            patch.setBirthday(request.birthday());
        }
        if (request.school() != null) {
            patch.setSchool(request.school().trim());
        }
        if (request.zgId() != null) {
            patch.setZgId(request.zgId().trim());
        }
        if (request.tagJson() != null) {
            patch.setTagsJson(request.tagJson().trim());
        }

        return patch;
    }

    /**
     * Convert a User entity to a ProfileResponse DTO.
     * This is used to return the updated user profile information to the client after a successful update.
     *
     * @param user The User entity from the database, containing the updated profile information
     * @return A ProfileResponse DTO containing the user's profile information to be sent back to the client
     */
    private static ProfileResponse convertToProfileResponse(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatar(),
                user.getBio(),
                user.getZgId(),
                user.getGender(),
                user.getBirthday(),
                user.getSchool(),
                user.getPhone(),
                user.getEmail(),
                user.getTagsJson()
        );
    }
}
