package com.foodordering.menu_microservice.controller;

import com.foodordering.menu_microservice.dto.*;
import com.foodordering.menu_microservice.service.MenuItemService;
import com.foodordering.menu_microservice.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final MenuItemService menuItemService;

    // GET /api/menu/restaurants — List / search restaurants (Public)
    @GetMapping("/restaurants")
    public ResponseEntity<List<RestaurantSummaryResponse>> getAllRestaurants(
            @RequestParam(required = false) String name) {
        if (name != null && !name.isBlank()) {
            return ResponseEntity.ok(restaurantService.searchByName(name));
        }
        return ResponseEntity.ok(restaurantService.getAllRestaurants());
    }

    // GET /api/menu/restaurants/{id} — Get restaurant + full menu (Public)
    @GetMapping("/restaurants/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable UUID id) {
        return ResponseEntity.ok(restaurantService.getRestaurantById(id));
    }

    // POST /api/menu/restaurants — Create restaurant (ADMIN)
    @PostMapping("/restaurants")
    public ResponseEntity<RestaurantResponse> createRestaurant(
            @Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.createRestaurant(request));
    }

    // PUT /api/menu/restaurants/{id} — Update restaurant / toggle active (ADMIN)
    @PutMapping("/restaurants/{id}")
    public ResponseEntity<RestaurantResponse> updateRestaurant(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.ok(restaurantService.updateRestaurant(id, request));
    }

    // POST /api/menu/restaurants/{id}/items — Add menu item (ADMIN)
    @PostMapping("/restaurants/{id}/items")
    public ResponseEntity<MenuItemResponse> addMenuItem(
            @PathVariable UUID id,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuItemService.addMenuItem(id, request));
    }

    // PUT /api/menu/items/{id} — Update item price/availability (ADMIN)
    @PutMapping("/items/{id}")
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @PathVariable UUID id,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(menuItemService.updateMenuItem(id, request));
    }

    // GET /api/menu/items/{id} — Internal: fetch item for Order Service
    @GetMapping("/items/{id}")
    public ResponseEntity<MenuItemResponse> getMenuItem(@PathVariable UUID id) {
        return ResponseEntity.ok(menuItemService.getMenuItemById(id));
    }
}
