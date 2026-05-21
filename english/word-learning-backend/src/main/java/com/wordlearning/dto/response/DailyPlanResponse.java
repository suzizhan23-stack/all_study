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
public class DailyPlanResponse {
    private String date;
    private int total;
    private int completed;
    private List<WordEntry> words;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WordEntry {
        private String id;
        private String wordId;
        private String word;
        private String phoneticUk;
        private String pos;
        private String posLabel;
        private String meaningCn;
        private boolean isCompleted;
        private String entrySource;
        private int sortOrder;
        private List<CollocationCompact> collocations;
        private List<PrepCompact> preps;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CollocationCompact {
        private String text;
        private int frequency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PrepCompact {
        private String pattern;
        private String preposition;
    }
}
