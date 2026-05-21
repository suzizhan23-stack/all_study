package com.wordlearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GeneratePlanRequest {
    @NotBlank
    private String wordBookId;
    @NotBlank
    private String strategyId;
    @NotBlank
    private String date;
    private Integer count;
}
