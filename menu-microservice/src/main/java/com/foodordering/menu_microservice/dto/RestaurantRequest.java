package com.foodordering.menu_microservice.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record RestaurantRequest(
        @NotBlank(message = "Restaurant name is required") String name,
        String description,
        UUID managerId
) {}