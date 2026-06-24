package com.foodordering.order_microservice.service;

import com.foodordering.order_microservice.dto.CreateOrderRequest;
import com.foodordering.order_microservice.dto.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(UUID userId, CreateOrderRequest request);

    OrderResponse getOrder(Long id);

    List<OrderResponse> getOrdersByUser(UUID userId);

    OrderResponse confirmOrder(Long id);

    OrderResponse completeOrder(Long id);

    OrderResponse cancelOrder(Long id);
}
