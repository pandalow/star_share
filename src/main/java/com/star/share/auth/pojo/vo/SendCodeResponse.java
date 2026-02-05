package com.star.share.auth.pojo.vo;

import com.star.share.auth.enumerate.VerificationScene;

/**
 * Send code response
 * @param identifier
 * @param scene
 * @param expireSeconds
 */
public record SendCodeResponse(
        String identifier,
        VerificationScene scene,
        int expireSeconds
) {
}
