package com.wordlearning.controller;

import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.dto.response.WordBookListResponse;
import com.wordlearning.dto.response.WordBookWordsResponse;
import com.wordlearning.service.WordBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/word-books")
@RequiredArgsConstructor
public class WordBookController {

    private final WordBookService wordBookService;

    @GetMapping
    public ApiResponse<WordBookListResponse> getWordBooks(@RequestParam(required = false) String difficultyLevel) {
        return ApiResponse.success(wordBookService.getWordBooks(difficultyLevel));
    }

    @GetMapping("/{id}/words")
    public ApiResponse<WordBookWordsResponse> getWordBookWords(@PathVariable("id") String uuid,
                                                                @RequestParam(required = false) String pos,
                                                                @RequestParam(required = false) String letter,
                                                                @RequestParam(required = false) String search,
                                                                @RequestParam(defaultValue = "1") int page,
                                                                @RequestParam(defaultValue = "30") int size) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(wordBookService.getWordBookWords(userId, uuid, pos, letter, search, page, size));
    }

    @GetMapping("/pos-categories")
    public ApiResponse<List<Map<String, Object>>> getPosCategories() {
        return ApiResponse.success(wordBookService.getPosCategories());
    }
}
