package com.foodordering.menu_microservice.dto;

import java.util.List;
import java.util.UUID;

public record RestaurantResponse(
        UUID id,
        String name,
        String description,
        UUID managerId,
        boolean isActive,
        List<MenuItemResponse> menuItems
) {}