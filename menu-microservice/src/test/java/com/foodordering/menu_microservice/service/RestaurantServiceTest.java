package com.foodordering.menu_microservice.service;

import com.foodordering.menu_microservice.dto.*;
import com.foodordering.menu_microservice.entity.Restaurant;
import com.foodordering.menu_microservice.exception.ResourceNotFoundException;
import com.foodordering.menu_microservice.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private MenuItemService menuItemService;
    @InjectMocks
    private RestaurantService restaurantService;
    private UUID restaurantId;
    private Restaurant restaurant;
    private RestaurantRequest request;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();

        restaurant = Restaurant.builder()
                .name("Pizza Palace")
                .description("Best pizza in town")
                .managerId(UUID.randomUUID())
                .build();

        setId(restaurant, restaurantId);
        restaurant.setActive(true);

        request = new RestaurantRequest("Pizza Palace", "Best pizza in town", restaurant.getManagerId());
    }

    @Test
    void createRestaurant_shouldSaveAndReturnResponse() {
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(restaurant);
        when(menuItemService.getItemsByRestaurant(restaurantId)).thenReturn(List.of());

        RestaurantResponse response = restaurantService.createRestaurant(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Pizza Palace");
        assertThat(response.description()).isEqualTo("Best pizza in town");
        verify(restaurantRepository, times(1)).save(any(Restaurant.class));
    }

    @Test
    void createRestaurant_shouldIncludeEmptyItemListWhenNoItemsExist() {
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(restaurant);
        when(menuItemService.getItemsByRestaurant(restaurantId)).thenReturn(List.of());

        RestaurantResponse response = restaurantService.createRestaurant(request);

        assertThat(response.menuItems()).isEmpty();
    }

    @Test
    void getAllRestaurants_shouldReturnListOfSummaries() {
        Restaurant second = Restaurant.builder()
                .name("Burger Barn")
                .description("Juicy burgers")
                .managerId(UUID.randomUUID())
                .build();
        setId(second, UUID.randomUUID());

        when(restaurantRepository.findAll()).thenReturn(List.of(restaurant, second));

        List<RestaurantSummaryResponse> result = restaurantService.getAllRestaurants();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(RestaurantSummaryResponse::name)
                .containsExactlyInAnyOrder("Pizza Palace", "Burger Barn");
    }

    @Test
    void getAllRestaurants_shouldReturnEmptyListWhenNoRestaurantsExist() {
        when(restaurantRepository.findAll()).thenReturn(List.of());

        List<RestaurantSummaryResponse> result = restaurantService.getAllRestaurants();

        assertThat(result).isEmpty();
    }

    @Test
    void getRestaurantById_shouldReturnResponseWhenFound() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemService.getItemsByRestaurant(restaurantId)).thenReturn(List.of());

        RestaurantResponse response = restaurantService.getRestaurantById(restaurantId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(restaurantId);
        assertThat(response.name()).isEqualTo("Pizza Palace");
    }

    @Test
    void getRestaurantById_shouldThrowWhenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(restaurantRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getRestaurantById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    void searchByName_shouldReturnMatchingRestaurants() {
        when(restaurantRepository.findByNameContainingIgnoreCase("pizza"))
                .thenReturn(List.of(restaurant));

        List<RestaurantSummaryResponse> result = restaurantService.searchByName("pizza");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Pizza Palace");
    }

    @Test
    void searchByName_shouldReturnEmptyListWhenNoMatch() {
        when(restaurantRepository.findByNameContainingIgnoreCase("sushi"))
                .thenReturn(List.of());

        List<RestaurantSummaryResponse> result = restaurantService.searchByName("sushi");

        assertThat(result).isEmpty();
    }

    @Test
    void updateRestaurant_shouldUpdateFieldsAndReturnResponse() {
        RestaurantRequest updateRequest = new RestaurantRequest("Pizza Palace Updated", "New desc", restaurant.getManagerId());

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);
        when(menuItemService.getItemsByRestaurant(restaurantId)).thenReturn(List.of());

        RestaurantResponse response = restaurantService.updateRestaurant(restaurantId, updateRequest);

        assertThat(response.name()).isEqualTo("Pizza Palace Updated");
        assertThat(response.description()).isEqualTo("New desc");
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void updateRestaurant_shouldThrowWhenRestaurantNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(restaurantRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.updateRestaurant(unknownId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknownId.toString());

        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void deleteRestaurant_shouldDeleteWhenFound() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        restaurantService.deleteRestaurant(restaurantId);

        verify(restaurantRepository).deleteById(restaurantId);
    }

    @Test
    void deleteRestaurant_shouldThrowWhenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(restaurantRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.deleteRestaurant(unknownId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(restaurantRepository, never()).deleteById(any());
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
}