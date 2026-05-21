package com.wordlearning.controller;

import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.dto.response.StrategyListResponse;
import com.wordlearning.service.WordBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final WordBookService wordBookService;

    @GetMapping
    public ApiResponse<StrategyListResponse> getStrategies() {
        return ApiResponse.success(wordBookService.getStrategies());
    }
}
