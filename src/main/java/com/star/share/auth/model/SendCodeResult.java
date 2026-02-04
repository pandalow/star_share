package com.star.share.auth.model;

import com.star.share.auth.enumerate.VerificationScene;

/**
 * Return Regulate result with identifier, scene, expire seconds
 * @param identifier
 * @param scene
 * @param expireSeconds
 */
public record SendCodeResult(String identifier, VerificationScene scene, int expireSeconds) {

}
