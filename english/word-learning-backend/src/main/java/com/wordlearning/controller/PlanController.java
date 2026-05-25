package com.wordlearning.controller;

import com.wordlearning.dto.request.CreatePlanRequest;
import com.wordlearning.dto.request.GeneratePlanRequest;
import com.wordlearning.dto.request.JoinPlanRequest;
import com.wordlearning.dto.request.BatchPlanEntryRequest;
import com.wordlearning.dto.request.PlanEntryRequest;
import com.wordlearning.dto.request.SetCurrentWordBookRequest;
import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.dto.response.DailyPlanResponse;
import com.wordlearning.dto.response.PlanDatesResponse;
import com.wordlearning.dto.response.PlanResponse;
import com.wordlearning.entity.LearningPlan;
import com.wordlearning.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping("/active")
    public ApiResponse<PlanResponse> getActivePlan() {
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

    @PostMapping("/create")
    public ApiResponse<PlanResponse> createPlan(@Valid @RequestBody CreatePlanRequest req) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(planService.createPlan(userId, req.getWordBookId(), req.getStrategyId(), req.getDailyCount()));
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

    @PostMapping("/daily/entries/batch")
    public ApiResponse<Void> addBatchPlanEntries(@Valid @RequestBody BatchPlanEntryRequest req) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        planService.addBatchPlanEntries(userId, req);
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

    @PutMapping("/daily/entries/{id}/key-point")
    public ApiResponse<Void> toggleKeyPoint(@PathVariable("id") String uuid) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        planService.toggleKeyPoint(userId, uuid);
        return ApiResponse.success();
    }

    @PutMapping("/daily/entries/by-word/{wordId}/key-point")
    public ApiResponse<Void> toggleKeyPointByWord(@PathVariable("wordId") String wordUuid) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        planService.toggleKeyPointByWordId(userId, wordUuid);
        return ApiResponse.success();
    }

    @PostMapping("/daily/generate")
    public ApiResponse<Integer> generateDailyPlan(@Valid @RequestBody GeneratePlanRequest req) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(planService.generateDailyPlan(userId, req));
    }

    @PostMapping("/advance")
    public ApiResponse<PlanResponse> advanceDay() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(planService.advanceDay(userId));
    }

    @PutMapping("/current-wordbook")
    public ApiResponse<PlanResponse> setCurrentWordBook(@Valid @RequestBody SetCurrentWordBookRequest req) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(planService.setCurrentWordBook(userId, req.getWordBookId(), req.getStrategyId(), req.getDailyCount()));
    }
}
