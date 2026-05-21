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
public class PrepPatternResponse {
    private List<PrepItem> prepPatterns;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PrepItem {
        private String id;
        private String pattern;
        private String translation;
        private String preposition;
        private int frequency;
    }
}
