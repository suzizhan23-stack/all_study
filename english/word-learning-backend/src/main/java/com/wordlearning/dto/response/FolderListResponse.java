package com.wordlearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderListResponse {
    private List<FolderItem> folders;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FolderItem {
        private String id;
        private String name;
        private String category;
        private boolean isDefault;
        private boolean isPublic;
        private int itemCount;
        private int sortOrder;
        private String createdAt;
    }
}
