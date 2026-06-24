package com.foodordering.order_microservice.service;

import com.foodordering.order_microservice.client.MenuClient;
import com.foodordering.order_microservice.client.UserClient;
import com.foodordering.order_microservice.dto.*;
import com.foodordering.order_microservice.entity.*;
import com.foodordering.order_microservice.exception.*;
import com.foodordering.order_microservice.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MenuClient menuClient;
    private final UserClient userClient;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            MenuClient menuClient,
            UserClient userClient
    ) {
        this.orderRepository = orderRepository;
        this.menuClient = menuClient;
        this.userClient = userClient;
    }

    @Override
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {

        if (!userClient.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.CREATED);

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {

            MenuItemDto menuItem = menuClient.getMenuItem(itemRequest.menuItemId());

            OrderItem item = new OrderItem();
            item.setMenuItemId(menuItem.id());
            item.setItemName(menuItem.name());
            item.setUnitPrice(menuItem.price());
            item.setQuantity(itemRequest.quantity());

            order.addItem(item);

            totalPrice = totalPrice.add(
                    menuItem.price().multiply(BigDecimal.valueOf(itemRequest.quantity()))
            );
        }

        order.setTotalPrice(totalPrice);

        Order saved = orderRepository.save(order);

        return map(saved);
    }

    @Override
    public OrderResponse getOrder(Long id) {
        return map(findOrder(id));
    }

    @Override
    public List<OrderResponse> getOrdersByUser(UUID userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public OrderResponse confirmOrder(Long id) {

        Order order = findOrder(id);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOrderStateException("Only CREATED orders can be confirmed");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        return map(order);
    }

    @Override
    public OrderResponse completeOrder(Long id) {

        Order order = findOrder(id);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException("Only CONFIRMED orders can be completed");
        }

        order.setStatus(OrderStatus.COMPLETED);
        return map(order);
    }

    @Override
    public OrderResponse cancelOrder(Long id) {

        Order order = findOrder(id);

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new InvalidOrderStateException("Completed orders cannot be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return map(order);
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private OrderResponse map(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getCreatedAt()
        );
    }
}
