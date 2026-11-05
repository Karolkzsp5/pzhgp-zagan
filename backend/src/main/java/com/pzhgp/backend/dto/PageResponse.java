package com.pzhgp.backend.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Strona wyników zwracana przez endpointy ze stronicowaniem.
 * Ma stały format JSON, bo Spring Data nie gwarantuje formatu zserializowanego obiektu {@link Page}.
 */
public record PageResponse<T>(
        List<T> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
