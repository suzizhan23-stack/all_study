package com.wordlearning.controller;

import com.wordlearning.dto.request.FolderRequest;
import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.dto.response.FolderItemsResponse;
import com.wordlearning.dto.response.FolderListResponse;
import com.wordlearning.service.FavoriteService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ApiResponse<FolderListResponse> getFolders(@RequestParam(required = false) String category) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(favoriteService.getFolders(userId, category));
    }

    @PostMapping
    public ApiResponse<FolderListResponse.FolderItem> createFolder(@Valid @RequestBody FolderRequest req) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(favoriteService.createFolder(userId, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateFolder(@PathVariable String id, @RequestBody FolderRequest req) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        favoriteService.updateFolder(userId, id, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFolder(@PathVariable String id) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        favoriteService.deleteFolder(userId, id);
        return ApiResponse.success();
    }

    @PutMapping("/reorder")
    public ApiResponse<Void> reorderFolders(@RequestBody Map<String, Object> body) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        @SuppressWarnings("unchecked")
        List<String> order = (List<String>) body.get("order");
        favoriteService.reorderFolders(userId, order);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/items")
    public ApiResponse<FolderItemsResponse> getFolderItems(@PathVariable String id,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int size,
                                                            @RequestParam(required = false) String sort) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(favoriteService.getFolderItems(userId, id, page, size, sort));
    }
}
