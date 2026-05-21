package com.wordlearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinPlanRequest {
    @NotBlank
    private String planId;
}
