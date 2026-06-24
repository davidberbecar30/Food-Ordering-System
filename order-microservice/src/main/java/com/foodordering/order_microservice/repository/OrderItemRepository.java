package com.foodordering.order_microservice.repository;

import com.foodordering.order_microservice.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
