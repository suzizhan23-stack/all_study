package com.wordlearning.controller;

import com.wordlearning.dto.request.FrequencyRequest;
import com.wordlearning.dto.request.NoteRequest;
import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.dto.response.WordDetailResponse;
import com.wordlearning.service.WordService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    @GetMapping("/{id}")
    public ApiResponse<WordDetailResponse> getWordDetail(@PathVariable String uuid) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(wordService.getWordDetail(userId, uuid));
    }

    @PutMapping("/{id}/frequency")
    public ApiResponse<Void> setFrequency(@PathVariable String uuid, @Valid @RequestBody FrequencyRequest body) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        wordService.setFrequency(userId, uuid, body.getFrequency());
        return ApiResponse.success();
    }

    @PutMapping("/{id}/note")
    public ApiResponse<Void> saveNote(@PathVariable String uuid, @Valid @RequestBody NoteRequest req) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        wordService.saveNote(userId, uuid, req);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/tags")
    public ApiResponse<Void> addTag(@PathVariable String uuid, @RequestBody Map<String, Object> body) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        wordService.addTag(userId, uuid, (String) body.get("tagId"));
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public ApiResponse<Void> removeTag(@PathVariable String uuid, @PathVariable String tagId) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        wordService.removeTag(userId, uuid, tagId);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/rating")
    public ApiResponse<Void> rateWord(@PathVariable String uuid, @RequestBody Map<String, Object> body) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        wordService.rateWord(userId, uuid, (int) body.get("rating"));
        return ApiResponse.success();
    }
}
