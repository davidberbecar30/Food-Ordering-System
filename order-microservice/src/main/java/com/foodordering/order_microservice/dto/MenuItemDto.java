package com.foodordering.order_microservice.dto;

import java.math.BigDecimal;

public record MenuItemDto(
        String id,
        String name,
        BigDecimal price
) {
}
