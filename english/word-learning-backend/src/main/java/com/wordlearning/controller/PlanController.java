package com.wordlearning.controller;

import com.wordlearning.dto.request.GeneratePlanRequest;
import com.wordlearning.dto.request.JoinPlanRequest;
import com.wordlearning.dto.request.PlanEntryRequest;
import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.dto.response.DailyPlanResponse;
import com.wordlearning.dto.response.PlanDatesResponse;
import com.wordlearning.entity.LearningPlan;
import com.wordlearning.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping("/active")
    public ApiResponse<Map<String, Object>> getActivePlan() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(planService.getActivePlan(userId));
    }

    @GetMapping("/templates")
    public ApiResponse<List<LearningPlan>> getPlanTemplates() {
        return ApiResponse.success(planService.getPlanTemplates());
    }

    @PostMapping("/join")
    public ApiResponse<Void> joinPlan(@Valid @RequestBody JoinPlanRequest req) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        planService.joinPlan(userId, req.getPlanId());
        return ApiResponse.success();
    }

    @GetMapping("/daily/words")
    public ApiResponse<DailyPlanResponse> getDailyWords(@RequestParam String date) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(planService.getDailyWords(userId, date));
    }

    @GetMapping("/daily/dates")
    public ApiResponse<PlanDatesResponse> getPlanDates(@RequestParam(defaultValue = "30") int limit) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(planService.getPlanDates(userId, limit));
    }

    @PostMapping("/daily/entries")
    public ApiResponse<Void> addPlanEntry(@Valid @RequestBody PlanEntryRequest req) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        planService.addPlanEntry(userId, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/daily/entries/{id}")
    public ApiResponse<Void> removePlanEntry(@PathVariable("id") String uuid) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        planService.removePlanEntry(userId, uuid);
        return ApiResponse.success();
    }

    @PutMapping("/daily/entries/{id}/complete")
    public ApiResponse<Void> markEntryComplete(@PathVariable("id") String uuid) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        planService.markEntryComplete(userId, uuid);
        return ApiResponse.success();
    }

    @PostMapping("/daily/generate")
    public ApiResponse<Integer> generateDailyPlan(@Valid @RequestBody GeneratePlanRequest req) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(planService.generateDailyPlan(userId, req));
    }
}
