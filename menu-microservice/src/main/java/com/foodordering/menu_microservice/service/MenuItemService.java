package com.foodordering.menu_microservice.service;

import com.foodordering.menu_microservice.dto.MenuItemRequest;
import com.foodordering.menu_microservice.dto.MenuItemResponse;
import com.foodordering.menu_microservice.entity.MenuItem;
import com.foodordering.menu_microservice.entity.Restaurant;
import com.foodordering.menu_microservice.exception.ResourceNotFoundException;
import com.foodordering.menu_microservice.repository.MenuItemRepository;
import com.foodordering.menu_microservice.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    public MenuItemResponse addMenuItem(UUID restaurantId, MenuItemRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        MenuItem item = MenuItem.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .isAvailable(request.isAvailable())
                .restaurant(restaurant)
                .build();

        return mapToResponse(menuItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getItemsByRestaurant(UUID restaurantId) {
        return menuItemRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItemById(UUID itemId) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + itemId));
        return mapToResponse(item);
    }

    public MenuItemResponse updateMenuItem(UUID itemId, MenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + itemId));
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setAvailable(request.isAvailable());
        return mapToResponse(menuItemRepository.save(item));
    }

    public void deleteMenuItem(UUID itemId) {
        menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + itemId));
        menuItemRepository.deleteById(itemId);
    }

    private MenuItemResponse mapToResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.isAvailable(),
                item.getRestaurant().getId()
        );
    }
}