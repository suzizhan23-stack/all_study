package com.wordlearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FavoriteRequest {
    @NotBlank
    private String folderId;
    @NotBlank
    private String entityType;
    @NotBlank
    private String entityId;
    private String note;
}
