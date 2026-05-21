package com.wordlearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlanEntryRequest {
    @NotBlank
    private String wordId;
    @NotBlank
    private String planDate;
}
