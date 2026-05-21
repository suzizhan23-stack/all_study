package com.wordlearning.controller;

import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.dto.response.LeaderboardResponse;
import com.wordlearning.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ApiResponse<LeaderboardResponse> getLeaderboard(@RequestParam(defaultValue = "global") String type,
                                                            @RequestParam(defaultValue = "100") int limit) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(leaderboardService.getLeaderboard(type, limit, userId));
    }
}
