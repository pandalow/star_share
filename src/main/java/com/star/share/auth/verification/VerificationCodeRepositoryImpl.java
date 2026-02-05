package com.star.share.auth.verification;

import com.star.share.auth.enumerate.VerificationCodeStatus;
import com.star.share.auth.pojo.VerificationCheckResult;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Base on Redis for implementing interface of VerificationCodeRepo
 *
 * <p>
 *     Using Hash map to store 'code', 'maxAttempts', 'attempts', TTL
 */
@Component
public class VerificationCodeRepositoryImpl implements VerificationCodeRepository{

    private static final String FIELD_CODE = "code";
    private static final String FIELD_MAX_ATTEMPTS = "maxAttempts";
    private static final String FIELD_ATTEMPTS = "attempts";

    private final StringRedisTemplate redisTemplate;

    public VerificationCodeRepositoryImpl(StringRedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    // Helper function
    /**
     * Generate Redis key
     * @param scene scene name
     * @param identifier email or phone number
     * @return key name
     */
    private static String buildKey(String scene, String identifier){
        return "auth:code:%s:%s".formatted(scene,identifier);
    }
    /**
     * parse string value, if failure return default value
     * @param value string
     * @param defaultValue default value
     * @return INT
     */
    private static int parseInt(String value, int defaultValue){
        if(value == null){
            return defaultValue;
        }
        try{
            return Integer.parseInt(value);
        }catch (NumberFormatException e){
            return defaultValue;
        }
    }

    /**
     * Save verification code to Redis Hash and set TTL
     * @param scene scene name
     * @param identifier phone number or email address
     * @param code verification code
     * @param ttl duration
     * @param maxAttempts
     */
    @Override
    public void saveCode(String scene, String identifier, String code, Duration ttl, int maxAttempts) {
        String key = buildKey(scene, identifier);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        try{
            ops.put(key, FIELD_CODE, code);
            ops.put(key, FIELD_MAX_ATTEMPTS, String.valueOf(maxAttempts));
            ops.put(key, FIELD_ATTEMPTS, "0");
        } catch (DataAccessException e){
            throw new RedisSystemException("Failed to save verification code", e);
        }
    }

    /**
     * Verification code is math, update the counter of attempts
     * @param scene scene name
     * @param identifier phone number or email address
     * @param code verification code
     * @return
     */
    @Override
    public VerificationCheckResult verify(String scene, String identifier, String code) {
        String key = buildKey(scene, identifier);
        HashOperations<String,String,String> ops = redisTemplate.opsForHash();
        Map<String,String> data = ops.entries(key);
        if(data.isEmpty()){
            return new VerificationCheckResult(VerificationCodeStatus.NOT_FOUND,0,0);
        }
        String storedCode = data.get(FIELD_CODE);
        int maxAttempts = parseInt(data.get(FIELD_MAX_ATTEMPTS), 5);
        int attempts = parseInt(data.get(FIELD_ATTEMPTS),0);

        if(attempts >= maxAttempts){
            return new VerificationCheckResult(VerificationCodeStatus.TOO_MANY_ATTEMPTS,attempts, maxAttempts);
        }

        if (Objects.equals(storedCode, code)){
            redisTemplate.delete(key);
            return new VerificationCheckResult(VerificationCodeStatus.SUCCESS, attempts, maxAttempts);
        }

        int updatedAttempts = attempts + 1;
        ops.put(key, FIELD_ATTEMPTS, String.valueOf(updatedAttempts));
        if(updatedAttempts >= maxAttempts){
            redisTemplate.expire(key, Duration.ofMinutes(39));
            return new VerificationCheckResult(VerificationCodeStatus.TOO_MANY_ATTEMPTS, updatedAttempts, maxAttempts);
        }

        return new VerificationCheckResult(VerificationCodeStatus.MISMATCH, updatedAttempts, attempts);
    }

    /**
     * Delete verification key
     * @param scene scene name
     * @param identifier phone number or email address
     */
    @Override
    public void invalidate(String scene, String identifier) {
        redisTemplate.delete(buildKey(scene, identifier));
    }

}
