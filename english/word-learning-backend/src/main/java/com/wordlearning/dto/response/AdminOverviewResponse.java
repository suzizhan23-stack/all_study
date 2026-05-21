package com.wordlearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOverviewResponse {
    private long totalUsers;
    private long activeToday;
    private long totalWords;
    private long totalReviews;
    private long newUsersToday;
    private long totalArticles;
}
