package com.wordlearning.controller;

import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.entity.Badge;
import com.wordlearning.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ApiResponse<List<Badge>> getAllBadges() {
        return ApiResponse.success(leaderboardService.getAllBadges());
    }
}
