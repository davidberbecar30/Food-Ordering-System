package com.foodordering.menu_microservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        boolean isAvailable,
        UUID restaurantId
) {}