package com.wordlearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanResponse {
    private String id;
    private String type;
    private WordBookInfo wordBook;
    private StrategyInfo strategy;
    private int dailyCount;
    private int currentDay;
    private int totalDays;
    private double pct;
    private int todayWords;
    private int todayCompleted;
    private int totalWords;
    private String startedAt;
    private boolean completed;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WordBookInfo {
        private String id;
        private String name;
        private int wordCount;
        private String difficultyLevel;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StrategyInfo {
        private String id;
        private String name;
        private String description;
    }
}
