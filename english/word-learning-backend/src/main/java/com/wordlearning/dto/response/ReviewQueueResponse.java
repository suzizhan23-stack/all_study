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
public class ReviewQueueResponse {
    private List<ReviewItem> queue;
    private int total;
    private int newWordsAvailable;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewItem {
        private String wordId;
        private String word;
        private String phoneticUk;
        private String phoneticUs;
        private String meaningCn;
        private String pos;
        private int stage;
        private int consecutiveCorrect;
        private double easeFactor;
        private int intervalDays;
        private String lastReviewedAt;
        private String nextReview;
    }
}
