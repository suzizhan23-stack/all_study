package com.wordlearning.controller;

import com.wordlearning.dto.request.ReviewResultRequest;
import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.dto.response.ReviewQueueResponse;
import com.wordlearning.dto.response.ReviewResultResponse;
import com.wordlearning.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/queue")
    public ApiResponse<ReviewQueueResponse> getQueue(@RequestParam(required = false) String mode,
                                                      @RequestParam(defaultValue = "20") int limit,
                                                      @RequestParam(required = false) String source) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(reviewService.getQueue(userId, mode, limit, source));
    }

    @PostMapping("/result")
    public ApiResponse<ReviewResultResponse> submitResult(@Valid @RequestBody ReviewResultRequest req) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(reviewService.submitResult(userId, req));
    }

    @GetMapping("/distractors")
    public ApiResponse<List<String>> getDistractors(@RequestParam String wordId,
                                                     @RequestParam(required = false) String pos,
                                                     @RequestParam(defaultValue = "3") int count) {
        return ApiResponse.success(reviewService.getDistractors(wordId, pos, count));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getReviewStats() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(reviewService.getReviewStats(userId));
    }
}
