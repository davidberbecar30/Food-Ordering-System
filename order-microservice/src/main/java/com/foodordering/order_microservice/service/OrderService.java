package com.foodordering.order_microservice.service;

import com.foodordering.order_microservice.dto.CreateOrderRequest;
import com.foodordering.order_microservice.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrder(Long id);

    List<OrderResponse> getOrdersByUser(Long userId);

    OrderResponse confirmOrder(Long id);

    OrderResponse completeOrder(Long id);

    OrderResponse cancelOrder(Long id);
}
