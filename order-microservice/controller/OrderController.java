package com.foodordering.order_microservice.controller;

import com.foodordering.order_microservice.dto.CreateOrderRequest;
import com.foodordering.order_microservice.dto.OrderResponse;
import com.foodordering.order_microservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(
            OrderService orderService
    ) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderResponse createOrder(
            @RequestBody @Valid CreateOrderRequest request
    ) {
        return orderService.createOrder(request);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(
            @PathVariable Long id
    ) {
        return orderService.getOrder(id);
    }

    @GetMapping("/user/{userId}")
    public List<OrderResponse> getOrdersByUser(
            @PathVariable Long userId
    ) {
        return orderService.getOrdersByUser(userId);
    }

    @PutMapping("/{id}/confirm")
    public OrderResponse confirm(
            @PathVariable Long id
    ) {
        return orderService.confirmOrder(id);
    }

    @PutMapping("/{id}/complete")
    public OrderResponse complete(
            @PathVariable Long id
    ) {
        return orderService.completeOrder(id);
    }

    @PutMapping("/{id}/cancel")
    public OrderResponse cancel(
            @PathVariable Long id
    ) {
        return orderService.cancelOrder(id);
    }
}