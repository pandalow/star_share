package com.star.share.profile.pojo;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ProfilePatchRequest(
        @NotBlank(message = "nickname must not be blank")
        @Size(max = 50, message = "nickname must be at most 50 characters") String nickname,
        @Size(max = 512, message = "bio must be at most 512 characters") String bio,
        @Pattern(regexp = "(?i)MALE|FEMALE|OTHER|UNKNOWN", message = "Gender must be in MALE|FEMALE|OTHER|UNKNOWN") String gender,
        @PastOrPresent(message = "birthday must be in the past or present") LocalDate birthday,
        @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "username must be 4-32 characters long and can only contain letters, numbers, and underscores") String zgId,
        @Size(max = 128, message = "school name must be in 128 chars") String school,
        String tagJson
) {
}
