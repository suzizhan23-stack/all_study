package com.wordlearning.controller;

import com.wordlearning.dto.request.ProgressRequest;
import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.dto.response.ArticleDetailResponse;
import com.wordlearning.dto.response.ArticleListResponse;
import com.wordlearning.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public ApiResponse<ArticleListResponse> getArticles(@RequestParam(required = false) Integer difficulty,
                                                         @RequestParam(required = false) String source,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(articleService.getArticles(userId, difficulty, source, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleDetailResponse> getArticleDetail(@PathVariable("id") String uuid) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(articleService.getArticleDetail(userId, uuid));
    }

    @PutMapping("/{id}/progress")
    public ApiResponse<Void> saveProgress(@PathVariable("id") String uuid, @RequestBody ProgressRequest req) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        articleService.saveProgress(userId, uuid, req.getScrollPosition(), req.getReadingTimeSec());
        return ApiResponse.success();
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<Void> markComplete(@PathVariable("id") String uuid) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        articleService.markComplete(userId, uuid);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/lookup")
    public ApiResponse<Map<String, Object>> lookupWord(@PathVariable("id") String uuid, @RequestParam String word) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(articleService.lookupWord(userId, uuid, word));
    }
}
