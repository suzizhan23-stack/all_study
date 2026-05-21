package com.wordlearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDatesResponse {
    private List<DateItem> dates;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DateItem {
        private String date;
        private int count;
        private int completed;
    }
}
