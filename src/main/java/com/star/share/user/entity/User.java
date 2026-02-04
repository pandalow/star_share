package com.star.share.user.entity;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@RequiredArgsConstructor
public class User {
    private Long id;
    private String phone;
    private String email;
    private String passwordHash;
    private String nickname;
    private String avatar;
    private String bio;
    private String zgId;
    private String gender;
    private LocalDate birthday;
    private String school;
    private String tagsJson;
    private Instant createdAt;
    private Instant updatedAt;

}
