package com.star.share.auth.service;

import com.star.share.auth.enumerate.IdentifierType;
import com.star.share.auth.pojo.ClientInfo;
import com.star.share.auth.pojo.VerificationCheckResult;
import com.star.share.auth.pojo.dto.*;
import com.star.share.auth.pojo.vo.AuthResponse;
import com.star.share.auth.pojo.vo.AuthUserResponse;
import com.star.share.auth.pojo.vo.SendCodeResponse;
import com.star.share.auth.pojo.vo.TokenResponse;
import com.star.share.user.entity.User;

import java.util.Optional;

public interface AuthService {
    /**
     * Send code and return expire time and identifier
     * @param request Send code request contains identifier and scene
     * @return Send code response contains identifier, scene and expire seconds
     */
    SendCodeResponse sendCode(SendCodeRequest request);

    /**
     * Register user and return user info and token info
     * @param request Register request contains identifier type, identifier, code, password and agree terms
     * @param clientInfo Client info contains client id, client type and client ip
     * @return Auth response contains user info and token info
     */
    AuthResponse register(RegisterRequest request, ClientInfo clientInfo);

    /**
     * Login and sign token, return user info and token info
     * @param request Login request contains identifier, code and password
     * @param clientInfo Client info contains client id, client type and client ip
     * @return Auth response contains user info and token info
     */
    AuthResponse login(LoginRequest request, ClientInfo clientInfo);

    /**
     * Using refresh token to refresh access token, return new token info
     * @param request Token refresh request contains refresh token
     * @return Token response contains new access token, refresh token and expire seconds
     */
    TokenResponse refresh(TokenRefreshRequest request);

    /**
     * Logout and invalidate refresh token
     * @param refreshToken Refresh token to be invalidated
     */
    void logout(String refreshToken);

    /**
     * Using verification code to reset password
     * @param request Password reset request contains identifier, code and new password
     */
    void resetPassword(PasswordResetRequest request);

    /**
     * Get current user info by user id
     * @param userId User id
     * @return User info response contains user id, username, email and phone number
     */
    AuthUserResponse me(long userId);

}

