package com.wordlearning.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private WordBookRef wordBook;
    private List<WordEntry> words;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WordBookRef {
        private String id;
        private String name;
    }

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
        @JsonProperty("isCompleted") private boolean isCompleted;
        @JsonProperty("isKeyPoint") private boolean isKeyPoint;
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
        private String translation;
        private int frequency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PrepCompact {
        private String pattern;
        private String translation;
        private String preposition;
    }
}
