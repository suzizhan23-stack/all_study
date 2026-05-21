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
public class BadgeListResponse {
    private List<BadgeItem> badges;
    private int earnedCount;
    private int totalCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BadgeItem {
        private String id;
        private String name;
        private String icon;
        private String description;
        private String conditionDesc;
        private String conditionType;
        private int conditionValue;
        private boolean isEarned;
        private String earnedAt;
    }
}
