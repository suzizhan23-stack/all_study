package com.wordlearning.controller;

import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.entity.UserTag;
import com.wordlearning.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final WordService wordService;

    @GetMapping
    public ApiResponse<List<UserTag>> getTags() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(wordService.getTags(userId));
    }

    @PostMapping
    public ApiResponse<UserTag> createTag(@RequestBody Map<String, Object> body) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String tag = (String) body.get("tag");
        String color = (String) body.get("color");
        return ApiResponse.success(wordService.createTag(userId, tag, color));
    }
}
