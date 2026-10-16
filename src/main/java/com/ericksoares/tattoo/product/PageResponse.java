package com.ericksoares.tattoo.product;

import java.util.List;

record PageResponse<T>(
        List<T> data,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
