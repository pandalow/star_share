package com.star.share.auth.service;

import com.star.share.auth.enumerate.VerificationScene;
import com.star.share.auth.pojo.SendCodeResult;
import com.star.share.auth.pojo.VerificationCheckResult;

public interface VerificationService {

    /**
     * Send code to identifier
     * <p>
     *     Execute send intervals and daily limit checks,
     *     Generate random number code, save to storage,
     * </p>
     * @param scene verification scene
     * @param identifier identifier(phone or email)
     * @return send code result
     */
    SendCodeResult sendCode(VerificationScene scene, String identifier);

    /**
     * Verify code for identifier
     * @param scene verification scene
     * @param identifier    identifier(phone or email)
     * @param code      code to verify
     * @return BusinessException
     */
    VerificationCheckResult verify(VerificationScene scene, String identifier, String code);

    /**
     * Invalidate code for identifier
     * @param scene verification scene
     * @param identifier    identifier(phone or email)
     */
    void invalidate(VerificationScene scene, String identifier);


}
