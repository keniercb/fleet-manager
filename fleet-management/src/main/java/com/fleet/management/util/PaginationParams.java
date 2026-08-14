package com.fleet.management.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaginationParams {
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDir;
}
