package com.wordlearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewResultRequest {
    @NotBlank
    private String wordId;
    @NotBlank
    private String quizType;
    @NotNull
    private Boolean isCorrect;
    private Integer responseTimeMs;
    private String wrongAnswer;
}
