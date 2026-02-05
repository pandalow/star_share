package com.star.share.auth.pojo.vo;

import java.time.LocalDate;

/**
 * Authentication user response, including user info
 * @param id
 * @param nickname
 * @param avatar
 * @param phone
 * @param zhId
 * @param birthday
 * @param school
 * @param bio
 * @param gender
 * @param tagJson
 */
public record AuthUserResponse(
        Long id,
        String nickname,
        String avatar,
        String phone,
        String zhId,
        LocalDate birthday,
        String school,
        String bio,
        String gender,
        String tagJson

) {
}
