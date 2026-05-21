package com.wordlearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FolderRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String category;
    private Boolean isPublic;
}
