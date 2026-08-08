package com.lucky.mescore.common.page;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private long total;
    private long pageNum;
    private long pageSize;
    private long totalPages;
    private List<T> records;

    public static <T> PageResponse<T> of(long total, long pageNum, long pageSize, List<T> records) {
        long totalPages = (total + pageSize - 1) / pageSize;
        return new PageResponse<>(total, pageNum, pageSize, totalPages, records);
    }

    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(0L, 1L, 10L, 0L, Collections.emptyList());
    }
}
