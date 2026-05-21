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
public class FolderItemsResponse {
    private FolderRef folder;
    private List<FolderItemDetail> items;
    private PageResponse.Pagination pagination;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FolderRef {
        private String id;
        private String name;
        private String category;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FolderItemDetail {
        private String id;
        private String entityType;
        private String entityId;
        private String word;
        private String meaningCn;
        private String note;
        private String createdAt;
    }
}
