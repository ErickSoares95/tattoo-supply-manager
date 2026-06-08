package com.ericksoares.tattoo.product.application.dto.response;

import java.util.List;

public record PageResponse<T>(
        List<T> data,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
