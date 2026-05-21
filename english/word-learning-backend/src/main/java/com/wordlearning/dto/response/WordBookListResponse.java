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
public class WordBookListResponse {
    private List<BookItem> books;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookItem {
        private String id;
        private String name;
        private String description;
        private String difficultyLevel;
        private int wordCount;
        private String icon;
        private String color;
        private int sortOrder;
        private boolean isActive;
    }
}
