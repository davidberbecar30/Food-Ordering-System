package com.foodordering.menu_microservice.dto;

import java.util.UUID;
public record RestaurantSummaryResponse(
    UUID id,
    String name,
    String description,
    UUID managerId,
    boolean isActive
) {}
