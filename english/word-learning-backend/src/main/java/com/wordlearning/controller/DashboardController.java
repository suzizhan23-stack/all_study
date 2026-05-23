package com.wordlearning.controller;

import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.dto.response.DashboardResponse;
import com.wordlearning.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ApiResponse<DashboardResponse> getDashboard() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(dashboardService.getDashboard(userId));
    }

    @PutMapping("/recommendations/{id}/consume")
    public ApiResponse<Void> consumeRecommendation(@PathVariable("id") String uuid) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        dashboardService.consumeRecommendation(userId, uuid);
        return ApiResponse.success();
    }
}
