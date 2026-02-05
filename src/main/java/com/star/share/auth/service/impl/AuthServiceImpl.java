package com.star.share.auth.service.impl;

import com.star.share.auth.audit.LoginLogService;
import com.star.share.auth.config.AuthProperties;
import com.star.share.auth.enumerate.IdentifierType;
import com.star.share.auth.enumerate.VerificationCodeStatus;
import com.star.share.auth.enumerate.VerificationScene;
import com.star.share.auth.pojo.ClientInfo;
import com.star.share.auth.pojo.SendCodeResult;
import com.star.share.auth.pojo.VerificationCheckResult;
import com.star.share.auth.pojo.dto.*;
import com.star.share.auth.pojo.vo.AuthResponse;
import com.star.share.auth.pojo.vo.AuthUserResponse;
import com.star.share.auth.pojo.vo.SendCodeResponse;
import com.star.share.auth.pojo.vo.TokenResponse;
import com.star.share.auth.service.AuthService;
import com.star.share.auth.service.VerificationService;
import com.star.share.auth.token.TokenPair;
import com.star.share.auth.utils.IdentifierValidator;
import com.star.share.common.exception.BusinessException;
import com.star.share.common.exception.ErrorCode;
import com.star.share.auth.token.JwtService;
import com.star.share.auth.token.RefreshTokenRepository;
import com.star.share.user.entity.User;
import com.star.share.user.service.UserService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final VerificationService verificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginLogService loginLogService;
    private final AuthProperties authProperties;


    /**
     * Send code and return expire time and identifier
     *
     * @param request Send code request contains identifier and scene
     * @return Send code response contains identifier, scene and expire seconds
     */
    @Override
    public SendCodeResponse sendCode(SendCodeRequest request) {
        validateIdentifier(request.identifierType(), request.identifier());
        String normalizedIdentifier = normalizeIdentifier(request.identifierType(), request.identifier());

        boolean exists = identifierExists(request.identifierType(), normalizedIdentifier);

        if (request.scene() == VerificationScene.REGISTER && exists) {
            throw new BusinessException(ErrorCode.IDENTIFIER_EXISTS);
        }

        if (request.scene() == VerificationScene.LOGIN ||
                request.scene() == VerificationScene.RESET_PASSWORD && !exists
        ) {
            throw new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND);
        }
        SendCodeResult result = verificationService.sendCode(request.scene(), normalizedIdentifier);
        return new SendCodeResponse(result.identifier(), result.scene(), result.expireSeconds());
    }

    ;

    /**
     * Register user and return user info and token info
     *
     * @param request    Register request contains identifier type, identifier, code, password and agree terms
     * @param clientInfo Client info contains client id, client type and client ip
     * @return Auth response contains user info and token info
     */
    public AuthResponse register(RegisterRequest request, ClientInfo clientInfo) {
        if (!request.agreeTerms()) {
            throw new BusinessException(ErrorCode.TERMS_NOT_ACCEPTED);
        }
        validateIdentifier(request.identifierType(), request.identifier());
        String normalizeIdentifier = normalizeIdentifier(request.identifierType(), request.identifier());
        if (identifierExists(request.identifierType(), normalizeIdentifier)) {
            throw new BusinessException(ErrorCode.IDENTIFIER_EXISTS);
        }
        ensureVerificationSuccess(
                verificationService.verify(
                        VerificationScene.REGISTER,
                        normalizeIdentifier,
                        request.code()
                ));
        User user = User.builder()
                .phone(request.identifierType() == IdentifierType.PHONE ? normalizeIdentifier : null)
                .email(request.identifierType() == IdentifierType.EMAIL ? normalizeIdentifier : null)
                .nickname(generateNickname())
                .avatar("https://star-share.oss-cn-shanghai.aliyuncs.com/default-avatar.png")
                .bio(null)
                .tagsJson("[]")
                .build();

        if (StringUtils.hasText(request.password())) {
            validatePassword(request.password());
            user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        }

        userService.createUser(user);
        TokenPair tokenPair = jwtService.tokenPair(user);
        saveRefreshToken(user.getId(), tokenPair);
        loginLogService.record(user.getId(),
                normalizeIdentifier,
                "REGISTER",
                clientInfo.ip(),
                clientInfo.userAgent(),
                "SUCCESS");
        return new AuthResponse(mapUser(user), mapToken(tokenPair));
    }

    ;

    /**
     * Login and sign token, return user info and token info
     *
     * @param request    Login request contains identifier, code and password
     * @param clientInfo Client info contains client id, client type and client ip
     * @return Auth response contains user info and token info
     */
    public AuthResponse login(LoginRequest request, ClientInfo clientInfo) {
        validateIdentifier(request.identifierType(), request.identifier());
        String normalizeIdentifier = normalizeIdentifier(request.identifierType(), request.identifier());
        Optional<User> optionalUser = findUserByIdentifier(request.identifierType(), normalizeIdentifier);
        if (optionalUser.isEmpty()) {
            throw new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND);
        }

        User user = optionalUser.get();
        String channel;

        if (StringUtils.hasText(request.password())) {
            channel = "PASSWORD";
            if (!StringUtils.hasText(user.getPasswordHash()) ||
                    !passwordEncoder.matches(request.password(),
                            user.getPasswordHash())) {
                loginLogService.record(user.getId(),
                        normalizeIdentifier,
                        channel,
                        clientInfo.ip(),
                        clientInfo.userAgent(),
                        "FAILURE: INVALID PASSWORD");
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }
        } else if (StringUtils.hasText(request.code())) {
            channel = "CODE";
            ensureVerificationSuccess(
                    verificationService.verify(
                            VerificationScene.LOGIN,
                            normalizeIdentifier,
                            request.code()
                    ));
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Please Provide Password or Verification Code");
        }

        TokenPair tokenPair = jwtService.tokenPair(user);
        saveRefreshToken(user.getId(), tokenPair);
        loginLogService.record(user.getId(),
                normalizeIdentifier,
                channel,
                clientInfo.ip(),
                clientInfo.userAgent(),
                "SUCCESS");
        return new AuthResponse(mapUser(user), mapToken(tokenPair));
    }

    /**
     * Using refresh token to refresh access token, return new token info
     *
     * @param request Token refresh request contains refresh token
     * @return Token response contains new access token, refresh token and expire seconds
     */
    public TokenResponse refresh(TokenRefreshRequest request) {
        Jwt jwt = decodeRefreshToken(request.refreshToken());

        if (!Objects.equals("refresh", jwtService.extractTokenType(jwt))) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID, "Invalid token type");
        }

        long userId = jwtService.extractUserId(jwt);
        String tokenId = jwtService.extractTokenId(jwt);

        if (!refreshTokenRepository.isTokenValid(userId, tokenId)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID, "Refresh token is invalid or expired");
        }

        User user = findUserById(userId).orElseThrow(
                () -> new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND, "User not found")
        );
        TokenPair tokenPair = jwtService.tokenPair(user);
        refreshTokenRepository.revokeToken(userId, tokenId);
        saveRefreshToken(userId, tokenPair);

        return mapToken(tokenPair);
    }

    /**
     * Logout and invalidate refresh token
     *
     * @param refreshToken Refresh token to be invalidated
     */
    public void logout(String refreshToken) {
        decodeRefreshTokenSafely(refreshToken).ifPresent(jwt -> {
            if (Objects.equals("refresh", jwtService.extractTokenType(jwt))) {
                long userId = jwtService.extractUserId(jwt);
                String tokenId = jwtService.extractTokenId(jwt);
                refreshTokenRepository.revokeToken(userId, tokenId);
            }
        });
    }

    ;

    /**
     * Using verification code to reset password
     *
     * @param request Password reset request contains identifier, code and new password
     */
    public void resetPassword(PasswordResetRequest request) {
        validateIdentifier(request.identifierType(), request.identifier());
        validatePassword(request.newPassword());
        String normalizeIdentifier = normalizeIdentifier(request.identifierType(), request.identifier());
        User user = findUserByIdentifier(request.identifierType(), normalizeIdentifier)
                .orElseThrow(() -> new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND));
        ensureVerificationSuccess(verificationService.verify(
                VerificationScene.RESET_PASSWORD,
                normalizeIdentifier,
                request.code()
        ));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
        userService.updatePassword(user);
        refreshTokenRepository.revokeAllTokens(user.getId());
    }

    ;

    /**
     * Get current user info by user id
     *
     * @param userId User id
     * @return User info response contains user id, username, email and phone number
     */
    public AuthUserResponse me(long userId) {
        User user = findUserById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND, "User not found"));
        return mapUser(user);
    }

    ;

    // Helper functions

    /**
     * Ensure the verification check result is successful, otherwise throw exception with error message
     *
     * @param result Verification check result to be checked
     */
    private void ensureVerificationSuccess(VerificationCheckResult result) {
        if (result.isSuccess()) {
            return;
        }

        VerificationCodeStatus status = result.status();
        if (status == VerificationCodeStatus.NOT_FOUND || status == VerificationCodeStatus.EXPIRED) {
            throw new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND);
        }
        if (status == VerificationCodeStatus.MISMATCH) {
            throw new BusinessException(ErrorCode.VERIFICATION_MISMATCH);
        }
        if (status == VerificationCodeStatus.TOO_MANY_ATTEMPTS) {
            throw new BusinessException(ErrorCode.VERIFICATION_TOO_MANY_ATTEMPTS);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Verification failed");
    }

    ;

    /**
     * Validate the identifier format according to the identifier type, throw exception with error message if invalid
     *
     * @param type       Identifier type, such as email or phone
     * @param identifier Identifier value to be validated
     */
    private void validateIdentifier(IdentifierType type, String identifier) {
        if (type == IdentifierType.PHONE && !IdentifierValidator.isValidPhone(identifier)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid phone number format");
        }
        if (type == IdentifierType.EMAIL && !IdentifierValidator.isValidEmail(identifier)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid email format");
        }
    }

    ;

    /**
     * Validate the password format, throw exception with error message if invalid
     *
     * @param password Password value to be validated
     */
    private void validatePassword(String password){
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "Password cannot be blank");
        }
        String trimmed = password.trim();
        if (trimmed.length() < authProperties.getPassword().getMinLength()) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "Password Length should be: " + authProperties.getPassword().getMinLength() + "位");
        }
        boolean hasLetter = trimmed.chars().anyMatch(Character::isLetter);
        boolean hasDigit = trimmed.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "Password should contain both letters and digits");
        }
    }

    /**
     * Check if the identifier already exists in the system, return true if exists, otherwise false
     *
     * @param type       Identifier type, such as email or phone
     * @param identifier Identifier value to be checked
     * @return True if the identifier already exists, otherwise false
     */
    private boolean identifierExists(IdentifierType type, String identifier){
        return switch (type) {
            case PHONE -> userService.existsByPhone(identifier);
            case EMAIL -> userService.existsByEmail(identifier);
        };
    };

    /**
     * Normalize the identifier by trimming spaces and converting to lowercase for email type
     *
     * @param type       Identifier type, such as email or phone
     * @param identifier Identifier value to be normalized
     * @return Normalized identifier value
     */
    private String normalizeIdentifier(IdentifierType type, String identifier) {
        return switch (type) {
            case PHONE -> identifier.trim();
            case EMAIL -> identifier.trim().toLowerCase(Locale.ROOT);
        };
    }

    /**
     * Find user by identifier, return optional user if found, otherwise empty optional
     *
     * @param type       Identifier type, such as email or phone
     * @param identifier Identifier value to be searched
     * @return Optional user if found, otherwise empty optional
     */
    private Optional<User> findUserByIdentifier(IdentifierType type, String identifier) {
        return switch (type) {
            case PHONE -> userService.findByPhone(identifier);
            case EMAIL -> userService.findByEmail(identifier);
        };
    }

    /**
     * Save refresh token for user, used for refresh token management
     *
     * @param userId    user id
     * @param tokenPair token pair contains refresh token and refresh token expire time, used for refresh token management
     */
    private void saveRefreshToken(Long userId, TokenPair tokenPair) {
        Duration ttl = Duration.between(Instant.now(), tokenPair.refreshTokenExpireAt());
        if (ttl.isNegative()) {
            ttl = Duration.ZERO;
        }
        refreshTokenRepository.saveToken(userId, tokenPair.refreshTokenId(), ttl);
    }

    // Helper function to generate random nickname for new user
    private String generateNickname() {
        return "StarShareUser" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Map user entity to auth user response,
     * return auth user response contains user id, username, email and phone number
     *
     * @param user User entity to be mapped
     * @return Auth user response contains user id, username, email and phone number
     */
    private AuthUserResponse mapUser(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatar(),
                user.getPhone(),
                user.getZgId(),
                user.getBirthday(),
                user.getSchool(),
                user.getBio(),
                user.getGender(),
                user.getTagsJson()
        );
    }

    /**
     * Map token pair to token response,
     * return token response contains new access token, refresh token and expire seconds
     *
     * @param tokenPair Token pair to be mapped
     * @return Token response contains new access token, refresh token and expire seconds
     */
    private TokenResponse mapToken(TokenPair tokenPair) {
        return new TokenResponse(tokenPair.accessToken(),
                tokenPair.accessTokenExpireAt(),
                tokenPair.refreshToken(),
                tokenPair.refreshTokenExpireAt());
    }

    /**
     * Decode refresh token string to Jwt object, throw exception if token is invalid
     *
     * @return Decoded Jwt object of refresh token
     */
    private Jwt decodeRefreshToken(@NotBlank(message = "refresh token cannot be blank") String refreshToken) {
        try {
            return jwtService.decode(refreshToken);
        } catch (JwtException ex) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    /**
     * Find user by user id, return optional user if found, otherwise empty optional
     *
     * @param userId User id to be searched
     * @return Optional user if found, otherwise empty optional
     */
    private Optional<User> findUserById(long userId) {
        return userService.findById(userId);
    }

    /**
     * Decode refresh token string to Jwt object, return optional Jwt if token is valid, otherwise empty optional
     *
     * @param refreshToken Refresh token string to be decoded
     * @return Optional Jwt object of refresh token if token is valid, otherwise empty optional
     */
    private Optional<Jwt> decodeRefreshTokenSafely(String refreshToken) {
        try {
            return Optional.of(jwtService.decode(refreshToken));
        } catch (JwtException ex) {
            return Optional.empty();
        }
    }

}
