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
public class WordBookWordsResponse {
    private BookRef book;
    private Filters filters;
    private List<WordPreview> words;
    private PageResponse.Pagination pagination;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookRef {
        private String id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Filters {
        private List<PosCategory> posCategories;
        private List<String> letters;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PosCategory {
        private String label;
        private String key;
        private List<String> posList;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WordPreview {
        private String id;
        private String word;
        private String pos;
        private String meaningCn;
        private String firstLetter;
        private int difficulty;
        private int frequency;
        private boolean isInPlan;
        private boolean isCompleted;
    }
}
