package com.foodordering.order_microservice.dto;

import java.util.UUID;

public record TokenValidationResponse(
        boolean valid,
        UUID userId,
        String username,
        String role
) {}
