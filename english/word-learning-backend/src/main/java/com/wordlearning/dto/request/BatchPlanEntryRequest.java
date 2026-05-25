package com.wordlearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class BatchPlanEntryRequest {
    @NotEmpty
    private List<@NotBlank String> wordIds;
    @NotBlank
    private String planDate;
}
