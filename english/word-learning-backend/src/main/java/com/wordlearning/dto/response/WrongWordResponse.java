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
public class WrongWordResponse {
    private Stats stats;
    private List<WordGroup> words;
    private PageResponse.Pagination pagination;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Stats {
        private int totalWrongWords;
        private int recentDays;
        private TopWrong topWrongWord;
        private List<WeakType> weakTypes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopWrong {
        private String word;
        private int count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeakType {
        private String quizType;
        private int count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WordGroup {
        private String wordId;
        private String word;
        private String meaningCn;
        private int wrongCount;
        private String lastWrong;
        private List<LogEntry> logs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LogEntry {
        private String id;
        private String quizType;
        private String wrongAnswer;
        private String reviewedAt;
    }
}
