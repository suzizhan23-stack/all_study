package com.wordlearning.controller;

import com.wordlearning.dto.request.ProfileUpdateRequest;
import com.wordlearning.dto.response.ActivityResponse;
import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.dto.response.BadgeListResponse;
import com.wordlearning.dto.response.StrategyListResponse;
import com.wordlearning.dto.response.UserProfileResponse;
import com.wordlearning.service.UserService;
import com.wordlearning.service.WordBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final WordBookService wordBookService;

    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestBody ProfileUpdateRequest req) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.updateProfile(userId, req);
        return ApiResponse.success();
    }

    @GetMapping("/settings")
    public ApiResponse<Map<String, String>> getSettings() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(userService.getSettings(userId));
    }

    @PutMapping("/settings")
    public ApiResponse<Void> saveSettings(@RequestBody Map<String, Object> body) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        @SuppressWarnings("unchecked")
        Map<String, String> settings = (Map<String, String>) body.get("settings");
        userService.saveSettings(userId, settings);
        return ApiResponse.success();
    }

    @GetMapping("/activity")
    public ApiResponse<ActivityResponse> getActivity(@RequestParam(defaultValue = "7") int days) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(userService.getActivity(userId, days));
    }

    @GetMapping("/badges")
    public ApiResponse<BadgeListResponse> getBadges() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(userService.getBadges(userId));
    }

    @GetMapping("/default-strategy")
    public ApiResponse<StrategyListResponse.StrategyItem> getDefaultStrategy() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(wordBookService.getUserDefaultStrategy(userId));
    }

    @PutMapping("/default-strategy")
    public ApiResponse<Void> setDefaultStrategy(@RequestBody Map<String, Object> body) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        wordBookService.setUserDefaultStrategy(userId, (String) body.get("strategyId"));
        return ApiResponse.success();
    }

    @PutMapping("/stats/streak")
    public ApiResponse<Integer> getStreak() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(userService.getStreak(userId));
    }
}
