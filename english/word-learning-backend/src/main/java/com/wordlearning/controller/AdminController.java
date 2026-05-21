package com.wordlearning.controller;

import com.wordlearning.dto.response.AdminOverviewResponse;
import com.wordlearning.dto.response.ApiResponse;
import com.wordlearning.dto.response.PageResponse;
import com.wordlearning.entity.Word;
import com.wordlearning.repository.WordRepository;
import com.wordlearning.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final WordRepository wordRepository;

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewResponse> getOverview() {
        return ApiResponse.success(adminService.getOverview());
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<Map<String, Object>>> getUsers(@RequestParam(required = false) String keyword,
                                                                    @RequestParam(required = false) String role,
                                                                    @RequestParam(required = false) Boolean isActive,
                                                                    @RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.getUsers(keyword, role, isActive, page, size));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<Void> toggleUserStatus(@PathVariable String id, @RequestBody Map<String, Object> body) {
        adminService.toggleUserStatus(id, (boolean) body.get("isActive"));
        return ApiResponse.success();
    }

    @GetMapping("/words")
    public ApiResponse<PageResponse<Word>> getWords(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Page<Word> wordPage = wordRepository.findAll(PageRequest.of(page - 1, size));
        PageResponse<Word> result = PageResponse.of(wordPage.getContent(), page, size, wordPage.getTotalElements());
        return ApiResponse.success(result);
    }

    @PostMapping("/words")
    public ApiResponse<Word> createWord(@RequestBody Word word) {
        return ApiResponse.success(adminService.createWord(word));
    }

    @PutMapping("/words/{id}")
    public ApiResponse<Word> updateWord(@PathVariable String id, @RequestBody Word word) {
        return ApiResponse.success(adminService.updateWord(id, word));
    }

    @DeleteMapping("/words/{id}")
    public ApiResponse<Void> deleteWord(@PathVariable String id) {
        adminService.deleteWord(id);
        return ApiResponse.success();
    }

    @PostMapping("/words/batch-import")
    public ApiResponse<Void> batchImportWords(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> words = (List<Map<String, Object>>) body.get("words");
        String wordBookId = (String) body.get("wordBookId");
        adminService.batchImportWords(words, wordBookId);
        return ApiResponse.success();
    }

    @GetMapping("/feedback")
    public ApiResponse<PageResponse<Map<String, Object>>> getFeedback(@RequestParam(defaultValue = "1") int page,
                                                                       @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.getFeedback(page, size));
    }
}
