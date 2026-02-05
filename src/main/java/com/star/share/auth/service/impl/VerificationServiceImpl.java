package com.star.share.auth.service.impl;

import com.star.share.auth.config.AuthProperties;
import com.star.share.auth.enumerate.VerificationScene;
import com.star.share.auth.pojo.SendCodeResult;
import com.star.share.auth.pojo.VerificationCheckResult;
import com.star.share.auth.service.VerificationService;
import com.star.share.auth.verification.CodeSender;
import com.star.share.auth.verification.VerificationCodeRepository;
import com.star.share.common.exception.BusinessException;
import com.star.share.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final VerificationCodeRepository codeRepository;
    private final CodeSender codeSender;
    private final StringRedisTemplate stringRedisTemplate;
    private final AuthProperties properties;

    /**
     * Enforce send interval for identifier in scene
     * @param scene verification scene
     * @param identifier    identifier(phone or email)
     * @param interval   interval duration
     */
    private void enforceSendInterval(VerificationScene scene, String identifier, Duration interval) {
        if (interval.isZero() || interval.isNegative()) {
            return;
        }
        String key = "auth:code:last:" + scene.name() + ":" + identifier;
        String existing = stringRedisTemplate.opsForValue().get(key);
        if (existing != null) {
            throw new BusinessException(ErrorCode.VERIFICATION_RATE_LIMIT);
        }
        stringRedisTemplate.opsForValue().set(key, "1", interval);
    }
    /**
     * Enforce daily limit for identifier in scene
     * @param scene verification scene
     * @param identifier    identifier(phone or email)
     * @param limit     daily limit
     */
    private void enforceDailyLimit(VerificationScene scene, String identifier, int limit) {
        if (limit <= 0) {
            return;
        }
        String date = DAY_FORMAT.format(LocalDate.now());
        String key = "auth:code:count:" + scene.name() + ":" + identifier + ":" + date;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofDays(1));
        }
        if (count != null && count > limit) {
            throw new BusinessException(ErrorCode.VERIFICATION_DAILY_LIMIT);
        }
    }

    /**
     *  Generate defined length numeric code
     * @param length code length
     * @return numeric code
     */
    private static String generateNumericCode(int length) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int digit = RANDOM.nextInt(10);
            code.append(digit);
        }
        return code.toString();
    }

    /**
     * Send code to identifier
     * <p>
     *     Execute send intervals and daily limit checks,
     * @param scene verification scene
     * @param identifier identifier(phone or email)
     * @return result of send code
     * @throws BusinessException when verification parameters are invalid
     */
    @Override
    public SendCodeResult sendCode(VerificationScene scene, String identifier) {
        if (scene == null || !StringUtils.hasText(identifier)){
            throw new BusinessException(ErrorCode.BAD_REQUEST,"Please provide the correct verification");
        }
        AuthProperties.Verification cfg = properties.getVerification();
        enforceSendInterval(scene, identifier, cfg.getSendInterval());
        enforceDailyLimit(scene, identifier, cfg.getDailyLimit());

        String code = generateNumericCode(cfg.getCodeLength());
        codeRepository.saveCode(scene.name(), identifier, code, cfg.getTtl(), cfg.getMaxAttempts());
        codeSender.sendCode(scene.name(), identifier, code, (int) cfg.getTtl().toMinutes());
        return new SendCodeResult(identifier, scene, (int) cfg.getTtl().toSeconds());


    }
    /**
     * Verify code for identifier
     * @param scene verification scene
     * @param identifier    identifier(phone or email)
     * @param code      code to verify
     * @return BusinessException
     */
    @Override
    public VerificationCheckResult verify(VerificationScene scene, String identifier, String code) {
        if(scene == null || !StringUtils.hasText(identifier) || !StringUtils.hasText(code)){
            throw new BusinessException(ErrorCode.BAD_REQUEST,"Please provide the correct verification");
        }
        return codeRepository.verify(scene.name(), identifier, code);
    }

    /**
     * Invalidate code for identifier
     * @param scene verification scene
     * @param identifier    identifier(phone or email)
     */
    @Override
    public void invalidate(VerificationScene scene, String identifier) {
        codeRepository.invalidate(scene.name(), identifier);
    }

}
