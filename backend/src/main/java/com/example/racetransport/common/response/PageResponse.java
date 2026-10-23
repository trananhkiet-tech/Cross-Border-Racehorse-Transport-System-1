package com.example.racetransport.common.response;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Kết quả phân trang gọn, tránh lộ cấu trúc nội bộ của Spring Page. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
