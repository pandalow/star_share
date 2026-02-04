package com.star.share.auth.token;

import com.star.share.auth.config.AuthProperties;
import com.star.share.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * JWT Service for encoding and decoding JWT tokens, generating token pairs, and extracting claims
 */
@Service
@RequiredArgsConstructor
public class JwtService {
    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String CLAIM_USER_ID = "user_id";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final AuthProperties properties;
    private final Clock clock = Clock.systemUTC();

    /**
     * Decode JWT token
     *
     * @param token JWT token string
     * @return Decoded Jwt object
     */
    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    /**
     * Generate access token and refresh token pair for user
     * @param user user info to put in token
     * @return TokenPair containing access token and refresh token with expire time and refresh token id
     */
    public TokenPair tokenPair(User user) {
        String refreshTokenId = UUID.randomUUID().toString();
        Instant issuedAt = Instant.now(clock);
        Instant accessTokenExpireAt = issuedAt.plus(properties.getJwt().getAccessTokent1());
        Instant refreshTokenExpireAt = issuedAt.plus(properties.getJwt().getRefreshTokent1());

        String accessToken = encodeToken(user, issuedAt, accessTokenExpireAt, "access", UUID.randomUUID().toString());
        String refreshToken = encodeRefreshToken(user, issuedAt, refreshTokenExpireAt, refreshTokenId);
        return new TokenPair(accessToken, accessTokenExpireAt, refreshToken, refreshTokenExpireAt, refreshTokenId);
    }

    /**
     * encode JWT token with user info and token type
     *
     * @param user      user info to put in token
     * @param issueAt   token issue time
     * @param expireAt  token expire time
     * @param tokenType token type, can be "access" or "refresh"
     * @param tokenId   token id, used for refresh token management, can be stored in database or cache
     * @return Encoded JWT token string
     */
    private String encodeToken(User user, Instant issueAt, Instant expireAt, String tokenType, String tokenId) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(issueAt)
                .expiresAt(expireAt)
                .subject(String.valueOf(user.getId()))
                .id(tokenId)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .claim(CLAIM_USER_ID, user.getId())
                .claim("nickname", user.getNickname())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * Encode refresh token with user info and token type, used for refresh token management
     * @param user user info to put in token
     * @param issueAt token issue time
     * @param expireAt  token expire time
     * @param tokenId token id, used for refresh token management, can be stored in database or cache
     * @return Encoded JWT refresh token string
     */
    private String encodeRefreshToken(User user, Instant issueAt, Instant expireAt, String tokenId) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(issueAt)
                .expiresAt(expireAt)
                .subject(String.valueOf(user.getId()))
                .id(tokenId)
                .claim(CLAIM_TOKEN_TYPE, "refresh")
                .claim(CLAIM_USER_ID, user.getId())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * Extract user id from JWT token, used for authentication and authorization
     * @param jwt
     * @return User id, if claim is missing or invalid, throw IllegalArgumentException
     */
    public long extractUserId(Jwt jwt) {
        Object claims =  jwt.getClaim(CLAIM_USER_ID);

        // JWT claim can be number or string, depending on how it is encoded, so we need to handle both cases
        if(claims instanceof  Number number) {
            return number.longValue();
        }

        if(claims instanceof  String str){
            return Long.parseLong(str);
        }
        throw new IllegalArgumentException("Invalid user id in token");
    }

    /**
     * Extract token type from JWT token, used for token validation and refresh token management
     *
     * @param jwt Decoded JWT object
     * @return Token type string, can be "access" or "refresh", if claim is missing or invalid, return empty string
     */
    public String extractTokenType(Jwt jwt) {
        Object claim = jwt.getClaims().get(CLAIM_TOKEN_TYPE);
        return claim != null ? claim.toString() : "";
    }

    /**
     * Extract token id from JWT token, used for refresh token management
     *
     * @param jwt Decoded JWT object
     * @return Token id string, if claim is missing or invalid, return empty string
     */
    public String extractTokenId(Jwt jwt) {
        String tokenId = jwt.getId();
        return tokenId != null ? tokenId : "";
    }

}
