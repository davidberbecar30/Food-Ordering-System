package com.foodordering.order_microservice.repository;

import com.foodordering.order_microservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(UUID userId);
}
