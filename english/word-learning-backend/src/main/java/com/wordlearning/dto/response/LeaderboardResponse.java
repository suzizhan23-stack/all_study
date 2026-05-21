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
public class LeaderboardResponse {
    private String type;
    private Integer myRank;
    private List<LeaderEntry> leaderboard;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LeaderEntry {
        private int rank;
        private String userId;
        private String username;
        private String nickname;
        private String avatarUrl;
        private int xp;
        private int level;
        private int streakDays;
        private double accuracy;
    }
}
