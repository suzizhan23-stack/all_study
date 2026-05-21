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
public class CollocationResponse {
    private List<CollocationItem> collocations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CollocationItem {
        private String id;
        private String collocation;
        private String translation;
        private int frequency;
    }
}
