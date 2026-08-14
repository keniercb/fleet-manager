package com.fleet.management.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationUtils {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final String DEFAULT_SORT_BY = "id";
    private static final String DEFAULT_SORT_DIR = "asc";

    public static Pageable of(PaginationParams paginationParams) {
        int page = Math.max(DEFAULT_PAGE, paginationParams.getPage());
        int size = Math.max(1, paginationParams.getSize() != null ? paginationParams.getSize() : DEFAULT_SIZE);
        String sortDir = paginationParams.getSortDir() != null ? paginationParams.getSortDir() : DEFAULT_SORT_DIR;
        String sortBy = paginationParams.getSortBy() != null ? paginationParams.getSortBy() : DEFAULT_SORT_BY;
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }

    public static PaginationParams params(int page, int perPage, String sortBy, String sortOrder) {
        int pageNumber = Math.max(0, page - 1);
        int size = Math.max(1, perPage);
        return PaginationParams.builder()
                .page(pageNumber)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortOrder)
                .build();
    }
}
