package com.wordlearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> list;
    private Pagination pagination;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        private int page;
        private int size;
        private long total;
        private int totalPages;
    }

    public static <T> PageResponse<T> of(List<T> list, int page, int size, long total) {
        PageResponse<T> res = new PageResponse<>();
        res.setList(list);
        res.setPagination(new Pagination(page, size, total, (int) Math.ceil((double) total / size)));
        return res;
    }
}
