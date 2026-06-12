package com.foodordering.order_microservice.dto;

import com.foodordering.order_microservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Long userId,
        OrderStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt
) {
}