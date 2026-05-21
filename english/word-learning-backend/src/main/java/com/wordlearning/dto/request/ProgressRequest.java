package com.wordlearning.dto.request;

import lombok.Data;

@Data
public class ProgressRequest {
    private int scrollPosition;
    private Integer readingTimeSec;
}
