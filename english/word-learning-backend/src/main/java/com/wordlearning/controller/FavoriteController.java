package com.wordlearning.controller;

import com.wordlearning.dto.request.FavoriteRequest;
import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.service.FavoriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    public ApiResponse<Void> addFavorite(@Valid @RequestBody FavoriteRequest req) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        favoriteService.addFavorite(userId, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> removeFavorite(@PathVariable String id) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        favoriteService.removeFavorite(userId, id);
        return ApiResponse.success();
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDeleteFavorites(@RequestBody Map<String, Object> body) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        favoriteService.batchDeleteFavorites(userId, ids);
        return ApiResponse.success();
    }

    @PostMapping("/batch-tag")
    public ApiResponse<Void> batchTagWords(@RequestBody Map<String, Object> body) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        @SuppressWarnings("unchecked")
        List<String> wordIds = (List<String>) body.get("wordIds");
        String tagId = (String) body.get("tagId");
        favoriteService.batchTagWords(userId, wordIds, tagId);
        return ApiResponse.success();
    }
}
