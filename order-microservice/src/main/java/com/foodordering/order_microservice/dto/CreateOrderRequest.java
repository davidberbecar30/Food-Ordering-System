package com.foodordering.order_microservice.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(

        @NotEmpty
        List<OrderItemRequest> items
) {
}
