package com.wordlearning.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FrequencyRequest {
    @Min(1) @Max(100)
    private int frequency;
}
