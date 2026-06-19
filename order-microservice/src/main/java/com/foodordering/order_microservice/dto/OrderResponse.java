package com.foodordering.order_microservice.dto;

import com.foodordering.order_microservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        Long id,
        UUID userId,
        OrderStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt
) {
}
