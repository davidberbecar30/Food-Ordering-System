package com.foodordering.order_microservice.dto;

import java.math.BigDecimal;

public record MenuItemDto(
        Long id,
        String name,
        BigDecimal price
) {
}