package com.wordlearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private String id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String email;
    private String phone;
    private String role;
    private int level;
    private int xp;
    private int xpNextLevel;
    private int streakDays;
    private int longestStreak;
    private int totalWordsLearned;
    private int totalReviews;
    private int totalTimeSpentSec;
    private double accuracy;
    private String defaultStrategyId;
    private String createdAt;
}
