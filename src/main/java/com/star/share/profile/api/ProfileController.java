package com.star.share.profile.api;

import com.star.share.auth.token.JwtService;
import com.star.share.oss.service.OSSService;
import com.star.share.profile.pojo.ProfilePatchRequest;
import com.star.share.profile.pojo.ProfileResponse;
import com.star.share.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * ProfileController is a REST controller responsible
 * for handling HTTP requests related to user profile operations,
 * such as updating profile information and uploading avatar images.
 * It uses the ProfileService to perform business logic and the JwtService
 * to extract user information from JWT tokens for authentication purposes.
 */
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final JwtService jwtService;
    private final OSSService ossService;

    /**
     * Handle PATCH requests to update user profile information. This endpoint allows users to update their profile
     * information by sending a PATCH request with the fields they want to update.
     *
     * @param jwt     The JWT token containing the user's authentication information. The
     *                user ID will be extracted from this token to identify which user's profile to update.
     * @param request The profile patch request containing the fields to update. All fields are optional,
     *                but at least one must be provided for the update to proceed.
     * @return A ProfileResponse object containing the updated profile information after the update operation is successful.
     */
    @PatchMapping
    public ProfileResponse patch(@AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestBody ProfilePatchRequest request) {
        // Extract user ID from JWT token
        long userId = jwtService.extractUserId(jwt);

        return profileService.updateProfile(userId, request);
    }


    @PostMapping("/avatar")
    public ProfileResponse updateAvatar(@AuthenticationPrincipal Jwt jwt,
                                        @RequestPart("file") MultipartFile file) {
        // Extract user ID from JWT token
        long userId = jwtService.extractUserId(jwt);
        String url = ossService.uploadAvatar(userId, file);

        return profileService.updateAvatar(userId, url);
    }
}
