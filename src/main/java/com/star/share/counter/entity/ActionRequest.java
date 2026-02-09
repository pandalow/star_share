package com.star.share.counter.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request object for actions e.g. post, etc.
 * Contains the type of entity (e.g., post) and the specific entity ID (e.g., post123).
 */
@Data
public class ActionRequest {
    @NotBlank
    private String entityType; // like post
    @NotBlank
    private String entityId; // id of entity, e.g., post123
}
