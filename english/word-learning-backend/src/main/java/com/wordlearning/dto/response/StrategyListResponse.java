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
public class StrategyListResponse {
    private List<StrategyItem> strategies;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StrategyItem {
        private String id;
        private String name;
        private String description;
        private String type;
        private String config;
        private int sortOrder;
    }
}
