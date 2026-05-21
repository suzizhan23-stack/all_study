package com.wordlearning.controller;

import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.dto.response.WrongWordResponse;
import com.wordlearning.service.WrongWordService;
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
@RequestMapping("/api/wrong-words")
@RequiredArgsConstructor
public class WrongWordController {

    private final WrongWordService wrongWordService;

    @GetMapping
    public ApiResponse<WrongWordResponse> getWrongWords(@RequestParam(required = false) String quizType,
                                                         @RequestParam(defaultValue = "7") int days,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(wrongWordService.getWrongWords(userId, quizType, days, page, size));
    }

    @PostMapping("/review")
    public ApiResponse<List<String>> generateReviewQueue(@RequestBody Map<String, Object> body) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int limit = body.containsKey("limit") ? (int) body.get("limit") : 10;
        int days = body.containsKey("days") ? (int) body.get("days") : 7;
        return ApiResponse.success(wrongWordService.generateReviewQueue(userId, limit, days));
    }
}
