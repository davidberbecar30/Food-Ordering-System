package com.foodordering.menu_microservice.service;

import com.foodordering.menu_microservice.dto.MenuItemRequest;
import com.foodordering.menu_microservice.dto.MenuItemResponse;
import com.foodordering.menu_microservice.entity.MenuItem;
import com.foodordering.menu_microservice.entity.Restaurant;
import com.foodordering.menu_microservice.exception.ResourceNotFoundException;
import com.foodordering.menu_microservice.repository.MenuItemRepository;
import com.foodordering.menu_microservice.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @InjectMocks
    private MenuItemService menuItemService;
    private UUID restaurantId;
    private UUID itemId;
    private Restaurant restaurant;
    private MenuItem menuItem;
    private MenuItemRequest request;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        restaurant = Restaurant.builder()
                .name("Pizza Palace")
                .description("Best pizza in town")
                .managerId(UUID.randomUUID())
                .build();
        setId(restaurant, restaurantId);

        menuItem = MenuItem.builder()
                .name("Margherita")
                .description("Classic tomato and mozzarella")
                .price(new BigDecimal("12.99"))
                .isAvailable(true)
                .restaurant(restaurant)
                .build();
        setItemId(menuItem, itemId);

        request = new MenuItemRequest("Margherita", "Classic tomato and mozzarella", new BigDecimal("12.99"), true);
    }

    @Test
    void addMenuItem_shouldSaveAndReturnResponse() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(menuItem);

        MenuItemResponse response = menuItemService.addMenuItem(restaurantId, request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Margherita");
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("12.99"));
        assertThat(response.restaurantId()).isEqualTo(restaurantId);
        verify(menuItemRepository, times(1)).save(any(MenuItem.class));
    }

    @Test
    void addMenuItem_shouldThrowWhenRestaurantNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(restaurantRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemService.addMenuItem(unknownId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknownId.toString());

        verify(menuItemRepository, never()).save(any());
    }

    @Test
    void addMenuItem_shouldPersistAvailabilityFlagCorrectly() {
        MenuItemRequest unavailableRequest = new MenuItemRequest("Margherita", "desc", new BigDecimal("12.99"), false);
        MenuItem unavailableItem = MenuItem.builder()
                .name("Margherita").description("desc")
                .price(new BigDecimal("12.99")).isAvailable(false)
                .restaurant(restaurant).build();
        setItemId(unavailableItem, UUID.randomUUID());

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(unavailableItem);

        MenuItemResponse response = menuItemService.addMenuItem(restaurantId, unavailableRequest);

        assertThat(response.isAvailable()).isFalse();
    }

    @Test
    void getItemsByRestaurant_shouldReturnAllItemsForRestaurant() {
        MenuItem second = MenuItem.builder()
                .name("Pepperoni").description("Spicy pepperoni")
                .price(new BigDecimal("14.99")).isAvailable(true)
                .restaurant(restaurant).build();
        setItemId(second, UUID.randomUUID());

        when(menuItemRepository.findByRestaurantId(restaurantId)).thenReturn(List.of(menuItem, second));

        List<MenuItemResponse> result = menuItemService.getItemsByRestaurant(restaurantId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MenuItemResponse::name)
                .containsExactlyInAnyOrder("Margherita", "Pepperoni");
    }

    @Test
    void getItemsByRestaurant_shouldReturnEmptyListWhenNoItems() {
        when(menuItemRepository.findByRestaurantId(restaurantId)).thenReturn(List.of());

        List<MenuItemResponse> result = menuItemService.getItemsByRestaurant(restaurantId);

        assertThat(result).isEmpty();
    }

    @Test
    void getMenuItemById_shouldReturnResponseWhenFound() {
        when(menuItemRepository.findById(itemId)).thenReturn(Optional.of(menuItem));

        MenuItemResponse response = menuItemService.getMenuItemById(itemId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(itemId);
        assertThat(response.name()).isEqualTo("Margherita");
    }

    @Test
    void getMenuItemById_shouldThrowWhenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(menuItemRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemService.getMenuItemById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    void updateMenuItem_shouldUpdateAllFieldsAndReturnResponse() {
        MenuItemRequest updateRequest = new MenuItemRequest("Margherita XL", "Larger size", new BigDecimal("16.50"), true);

        when(menuItemRepository.findById(itemId)).thenReturn(Optional.of(menuItem));
        when(menuItemRepository.save(menuItem)).thenReturn(menuItem);

        MenuItemResponse response = menuItemService.updateMenuItem(itemId, updateRequest);

        assertThat(response.name()).isEqualTo("Margherita XL");
        assertThat(response.description()).isEqualTo("Larger size");
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("16.50"));
        verify(menuItemRepository).save(menuItem);
    }

    @Test
    void updateMenuItem_shouldToggleAvailability() {
        MenuItemRequest disableRequest = new MenuItemRequest("Margherita", "desc", new BigDecimal("12.99"), false);

        when(menuItemRepository.findById(itemId)).thenReturn(Optional.of(menuItem));
        when(menuItemRepository.save(menuItem)).thenReturn(menuItem);

        MenuItemResponse response = menuItemService.updateMenuItem(itemId, disableRequest);

        assertThat(response.isAvailable()).isFalse();
    }

    @Test
    void updateMenuItem_shouldThrowWhenItemNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(menuItemRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemService.updateMenuItem(unknownId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknownId.toString());

        verify(menuItemRepository, never()).save(any());
    }

    @Test
    void deleteMenuItem_shouldDeleteWhenFound() {
        when(menuItemRepository.findById(itemId)).thenReturn(Optional.of(menuItem));

        menuItemService.deleteMenuItem(itemId);

        verify(menuItemRepository).deleteById(itemId);
    }

    @Test
    void deleteMenuItem_shouldThrowWhenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(menuItemRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemService.deleteMenuItem(unknownId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(menuItemRepository, never()).deleteById(any());
    }

    private void setId(Restaurant r, UUID id) {
        try {
            var field = Restaurant.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(r, id);
        } catch (Exception e) {
            throw new RuntimeException("Could not set id on Restaurant", e);
        }
    }
    private void setItemId(MenuItem item, UUID id) {
        try {
            var field = MenuItem.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(item, id);
        } catch (Exception e) {
            throw new RuntimeException("Could not set id on MenuItem", e);
        }
    }
}