package com.dogasimsek.recipeapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/** One page of results plus pagination information (total pages, total elements, ...). */
public record PageResponse<T>(
        List<T> content,
        @Schema(description = "Current page number (starts at 0)", example = "0") int page,
        @Schema(description = "Page size", example = "10") int size,
        @Schema(description = "Number of all matching records", example = "12") long totalElements,
        @Schema(description = "Number of pages", example = "2") int totalPages,
        @Schema(example = "true") boolean first,
        @Schema(example = "false") boolean last,
        @Schema(example = "title: ASC,createdAt: DESC") String sort) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast(),
                page.getSort().toString());
    }
}
