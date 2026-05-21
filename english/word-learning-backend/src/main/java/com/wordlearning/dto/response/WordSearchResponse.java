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
public class WordSearchResponse {
    private List<SearchResult> list;
    private PageResponse.Pagination pagination;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchResult {
        private String id;
        private String word;
        private String phoneticUk;
        private String phoneticUs;
        private String pos;
        private String meaningCn;
        private String source;
        private int difficulty;
        private int frequency;
        private boolean isCollected;
    }
}
