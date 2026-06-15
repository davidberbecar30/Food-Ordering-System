package com.foodordering.menu_microservice.service;

import com.foodordering.menu_microservice.dto.RestaurantRequest;
import com.foodordering.menu_microservice.dto.RestaurantResponse;
import com.foodordering.menu_microservice.dto.RestaurantSummaryResponse;
import com.foodordering.menu_microservice.entity.Restaurant;
import com.foodordering.menu_microservice.exception.ResourceNotFoundException;
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
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemService menuItemService;

    public RestaurantResponse createRestaurant(RestaurantRequest request) {
        Restaurant restaurant = Restaurant.builder()
                .name(request.name())
                .description(request.description())
                .managerId(request.managerId())
                .build();
        return mapToResponse(restaurantRepository.save(restaurant));
    }

    @Transactional(readOnly = true)
    public List<RestaurantSummaryResponse> getAllRestaurants() {
        return restaurantRepository.findAll()
                .stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurantById(UUID id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
        return mapToResponse(restaurant);
    }

    @Transactional(readOnly = true)
    public List<RestaurantSummaryResponse> searchByName(String name) {
        return restaurantRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    public RestaurantResponse updateRestaurant(UUID id, RestaurantRequest request) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        restaurant.setManagerId(request.managerId());
        return mapToResponse(restaurantRepository.save(restaurant));
    }

    public void deleteRestaurant(UUID id) {
        restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
        restaurantRepository.deleteById(id);
    }

    private RestaurantResponse mapToResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getManagerId(),
                restaurant.isActive(),
                menuItemService.getItemsByRestaurant(restaurant.getId())
        );
    }
    private RestaurantSummaryResponse mapToSummary(Restaurant restaurant) {
        return new RestaurantSummaryResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getManagerId(),
                restaurant.isActive()
        );

    }
}