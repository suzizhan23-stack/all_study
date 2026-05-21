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
public class ArticleDetailResponse {
    private String id;
    private String title;
    private String content;
    private String contentType;
    private String author;
    private String sourceName;
    private String sourceUrl;
    private int difficulty;
    private Integer wordCount;
    private String publishedAt;
    private ArticleListResponse.ProgressInfo progress;
    private List<VocabItem> vocabulary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VocabItem {
        private String word;
        private String meaningCn;
        private int difficulty;
    }
}
