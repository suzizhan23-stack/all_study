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
public class DashboardResponse {
    private TodayProgress today;
    private Stats stats;
    private List<Recommendation> recommendations;
    private QuickCounts quick;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TodayProgress {
        private int wordsStudied;
        private int dailyGoal;
        private int pct;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Stats {
        private int streakDays;
        private int longestStreak;
        private int level;
        private int xp;
        private int xpNextLevel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Recommendation {
        private String id;
        private String entityType;
        private String entityId;
        private String word;
        private String reason;
        private boolean isConsumed;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuickCounts {
        private int dueReviewCount;
        private int unreadArticleCount;
        private int wrongWordCount;
    }
}
