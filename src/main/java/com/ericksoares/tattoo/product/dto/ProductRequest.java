package com.ericksoares.tattoo.product.dto;

import java.math.BigDecimal;

public  record ProductRequest(
        String name,
        BigDecimal price,
        Integer stock
) {}