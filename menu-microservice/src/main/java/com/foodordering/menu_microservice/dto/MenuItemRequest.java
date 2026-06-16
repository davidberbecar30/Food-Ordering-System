package com.foodordering.menu_microservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record MenuItemRequest(
        @NotBlank(message = "Item name is required") String name,
        String description,
        @NotNull(message = "Price is required") @Positive(message = "Price must be positive") BigDecimal price,
        boolean isAvailable
) {}