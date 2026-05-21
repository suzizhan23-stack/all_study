package com.wordlearning.controller;

import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.dto.response.SuggestResponse;
import com.wordlearning.dto.response.WordSearchResponse;
import com.wordlearning.entity.SearchHistory;
import com.wordlearning.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/suggest")
    public ApiResponse<SuggestResponse> suggest(@RequestParam String query,
                                                 @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(searchService.suggest(query, limit));
    }

    @GetMapping
    public ApiResponse<WordSearchResponse> search(@RequestParam String q,
                                                   @RequestParam(required = false) String source,
                                                   @RequestParam(required = false) String pos,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(searchService.search(userId, q, source, pos, page, size));
    }

    @PostMapping("/history")
    public ApiResponse<Void> saveHistory(@RequestBody Map<String, Object> body) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String query = (String) body.get("query");
        int resultCount = (int) body.get("resultCount");
        searchService.saveHistory(userId, query, resultCount);
        return ApiResponse.success();
    }

    @GetMapping("/history")
    public ApiResponse<List<SearchHistory>> getHistory(@RequestParam(defaultValue = "10") int limit) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(searchService.getHistory(userId, limit));
    }

    @DeleteMapping("/history")
    public ApiResponse<Void> clearHistory() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        searchService.clearHistory(userId);
        return ApiResponse.success();
    }
}
