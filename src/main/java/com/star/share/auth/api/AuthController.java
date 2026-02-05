package com.star.share.auth.api;

import com.star.share.auth.pojo.ClientInfo;
import com.star.share.auth.pojo.dto.*;
import com.star.share.auth.pojo.vo.AuthResponse;
import com.star.share.auth.pojo.vo.AuthUserResponse;
import com.star.share.auth.pojo.vo.SendCodeResponse;
import com.star.share.auth.pojo.vo.TokenResponse;
import com.star.share.auth.service.AuthService;
import com.star.share.auth.token.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    /**
     * Send verification code to user, used for login and registration
     *
     * @param request SendCodeRequest with identifier and scene
     *                contains:
     *                - identifierType: the type of identifier, can be email or phone number
     *                - identifier: the identifier of user, can be email or phone number
     *                - scene: the verification scene, can be login or registration
     * @return SendCodeResponse with identifier, scene and expire seconds
     */
    @PostMapping("/send-code")
    public SendCodeResponse sendCode(@Valid @RequestBody SendCodeRequest request) {
        return authService.sendCode(request);
    }

    /**
     * Register user with verification code, used for registration
     *
     * @param request     RegisterRequest with identifier, code and password
     * @param httpRequest HttpServletRequest for resolve client information
     * @return AuthResponse with access token and refresh token
     */
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, resolveClient(httpRequest));
    }

    /**
     * Login with verification code and password, used for login
     *
     * @param request     LoginRequest with identifier, code and password
     * @param httpRequest HttpServletRequest for resolve client information
     * @return AuthResponse with access token and refresh token
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, resolveClient(httpRequest));
    }

    /**
     * Refresh access token with refresh token, used for token refresh
     *
     * @param request contains refresh token string and refresh token id(jti), used for refresh token management
     * @return TokenResponse with new access token and refresh token
     */
    @PostMapping("/token/refresh")
    public TokenResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.refresh(request);
    }

    /**
     * Logout and invalidate refresh token, used for logout
     *
     * @param request contains refresh token string and refresh token id(jti), used for refresh token management
     * @return ResponseEntity with no content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * Reset password with verification code, used for password reset
     *
     * @param request PasswordResetRequest with identifier, code and new password
     * @return ResponseEntity with no content
     */
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current user info, used for get current user info
     *
     * @param jwt Jwt token contains user id and other claims, injected by Spring Security
     * @return AuthUserResponse with user id, username, email and phone number
     */
    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        return authService.me(userId);
    }


    // Helper function to resolve client information from HttpServletRequest, used for login and registration
    private ClientInfo resolveClient(HttpServletRequest request) {
        String ip = extractClientIp(request);
        String ua = request.getHeader("User-Agent");
        return new ClientInfo(ip, ua);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

}
