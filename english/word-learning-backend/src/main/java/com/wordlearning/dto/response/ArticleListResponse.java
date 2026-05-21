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
public class ArticleListResponse {
    private List<ArticleItem> list;
    private PageResponse.Pagination pagination;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ArticleItem {
        private String id;
        private String title;
        private String author;
        private String sourceName;
        private int difficulty;
        private Integer wordCount;
        private String coverImage;
        private String publishedAt;
        private List<String> tags;
        private ProgressInfo progress;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProgressInfo {
        private int scrollPosition;
        private boolean isCompleted;
        private int wordsLookedUp;
        private String lastReadAt;
    }
}
