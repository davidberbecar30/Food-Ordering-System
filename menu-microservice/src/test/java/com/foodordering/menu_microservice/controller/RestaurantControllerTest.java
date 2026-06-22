package com.foodordering.menu_microservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodordering.menu_microservice.dto.*;
import com.foodordering.menu_microservice.exception.ResourceNotFoundException;
import com.foodordering.menu_microservice.service.MenuItemService;
import com.foodordering.menu_microservice.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = RestaurantController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private RestaurantService restaurantService;
    @MockitoBean
    private MenuItemService menuItemService;
    private UUID restaurantId;
    private UUID itemId;
    private RestaurantResponse restaurantResponse;
    private RestaurantSummaryResponse summaryResponse;
    private MenuItemResponse menuItemResponse;
    private RestaurantRequest restaurantRequest;
    private MenuItemRequest menuItemRequest;


    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        restaurantResponse = new RestaurantResponse(
                restaurantId, "Pizza Palace", "Best pizza in town",
                UUID.randomUUID(), true, List.of()
        );

        summaryResponse = new RestaurantSummaryResponse(
                restaurantId, "Pizza Palace", "Best pizza in town",
                UUID.randomUUID(), true
        );

        menuItemResponse = new MenuItemResponse(
                itemId, "Margherita", "Classic tomato and mozzarella",
                new BigDecimal("12.99"), true, restaurantId
        );

        restaurantRequest = new RestaurantRequest(
                "Pizza Palace", "Best pizza in town", UUID.randomUUID()
        );

        menuItemRequest = new MenuItemRequest(
                "Margherita", "Classic tomato and mozzarella",
                new BigDecimal("12.99"), true
        );
    }

    @Test
    void getAllRestaurants_shouldReturn200WithList() throws Exception {
        when(restaurantService.getAllRestaurants()).thenReturn(List.of(summaryResponse));

        mockMvc.perform(get("/api/menu/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Pizza Palace"));

        verify(restaurantService).getAllRestaurants();
        verify(restaurantService, never()).searchByName(any());
    }

    @Test
    void getAllRestaurants_shouldReturn200WithEmptyList() throws Exception {
        when(restaurantService.getAllRestaurants()).thenReturn(List.of());

        mockMvc.perform(get("/api/menu/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllRestaurants_withNameParam_shouldCallSearchByName() throws Exception {
        when(restaurantService.searchByName("pizza")).thenReturn(List.of(summaryResponse));

        mockMvc.perform(get("/api/menu/restaurants").param("name", "pizza"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Pizza Palace"));

        verify(restaurantService).searchByName("pizza");
        verify(restaurantService, never()).getAllRestaurants();
    }

    @Test
    void getAllRestaurants_withBlankNameParam_shouldCallGetAll() throws Exception {
        when(restaurantService.getAllRestaurants()).thenReturn(List.of(summaryResponse));

        mockMvc.perform(get("/api/menu/restaurants").param("name", "   "))
                .andExpect(status().isOk());

        verify(restaurantService).getAllRestaurants();
        verify(restaurantService, never()).searchByName(any());
    }

    @Test
    void getRestaurant_shouldReturn200WhenFound() throws Exception {
        when(restaurantService.getRestaurantById(restaurantId)).thenReturn(restaurantResponse);

        mockMvc.perform(get("/api/menu/restaurants/{id}", restaurantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(restaurantId.toString()))
                .andExpect(jsonPath("$.name").value("Pizza Palace"))
                .andExpect(jsonPath("$.isActive").value(true));    }

    @Test
    void getRestaurant_shouldReturn404WhenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(restaurantService.getRestaurantById(unknownId))
                .thenThrow(new ResourceNotFoundException("Restaurant not found with id: " + unknownId));

        mockMvc.perform(get("/api/menu/restaurants/{id}", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRestaurant_shouldReturn201WithBody() throws Exception {
        when(restaurantService.createRestaurant(any(RestaurantRequest.class))).thenReturn(restaurantResponse);

        mockMvc.perform(post("/api/menu/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restaurantRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pizza Palace"))
                .andExpect(jsonPath("$.description").value("Best pizza in town"));

        verify(restaurantService).createRestaurant(any(RestaurantRequest.class));
    }

    @Test
    void createRestaurant_shouldReturn400WhenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/menu/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(restaurantService, never()).createRestaurant(any());
    }

    @Test
    void updateRestaurant_shouldReturn200WithUpdatedBody() throws Exception {
        RestaurantResponse updated = new RestaurantResponse(
                restaurantId, "Pizza Palace Updated", "New desc",
                restaurantRequest.managerId(), true, List.of()
        );
        when(restaurantService.updateRestaurant(eq(restaurantId), any(RestaurantRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/menu/restaurants/{id}", restaurantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restaurantRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pizza Palace Updated"));
    }

    @Test
    void updateRestaurant_shouldReturn404WhenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(restaurantService.updateRestaurant(eq(unknownId), any(RestaurantRequest.class)))
                .thenThrow(new ResourceNotFoundException("Restaurant not found with id: " + unknownId));

        mockMvc.perform(put("/api/menu/restaurants/{id}", unknownId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restaurantRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addMenuItem_shouldReturn201WithBody() throws Exception {
        when(menuItemService.addMenuItem(eq(restaurantId), any(MenuItemRequest.class))).thenReturn(menuItemResponse);

        mockMvc.perform(post("/api/menu/restaurants/{id}/items", restaurantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuItemRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Margherita"))
                .andExpect(jsonPath("$.price").value(12.99))
                .andExpect(jsonPath("$.restaurantId").value(restaurantId.toString()));

        verify(menuItemService).addMenuItem(eq(restaurantId), any(MenuItemRequest.class));
    }

    @Test
    void addMenuItem_shouldReturn404WhenRestaurantNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(menuItemService.addMenuItem(eq(unknownId), any(MenuItemRequest.class)))
                .thenThrow(new ResourceNotFoundException("Restaurant not found with id: " + unknownId));

        mockMvc.perform(post("/api/menu/restaurants/{id}/items", unknownId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuItemRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateMenuItem_shouldReturn200WithUpdatedBody() throws Exception {
        MenuItemResponse updated = new MenuItemResponse(
                itemId, "Margherita XL", "Larger size",
                new BigDecimal("16.50"), true, restaurantId
        );
        when(menuItemService.updateMenuItem(eq(itemId), any(MenuItemRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/menu/items/{id}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuItemRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Margherita XL"))
                .andExpect(jsonPath("$.price").value(16.50));
    }

    @Test
    void updateMenuItem_shouldReturn404WhenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(menuItemService.updateMenuItem(eq(unknownId), any(MenuItemRequest.class)))
                .thenThrow(new ResourceNotFoundException("Menu item not found with id: " + unknownId));

        mockMvc.perform(put("/api/menu/items/{id}", unknownId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuItemRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMenuItem_shouldReturn200WhenFound() throws Exception {
        when(menuItemService.getMenuItemById(itemId)).thenReturn(menuItemResponse);

        mockMvc.perform(get("/api/menu/items/{id}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.name").value("Margherita"))
                .andExpect(jsonPath("$.isAvailable").value(true));
    }

    @Test
    void getMenuItem_shouldReturn404WhenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(menuItemService.getMenuItemById(unknownId))
                .thenThrow(new ResourceNotFoundException("Menu item not found with id: " + unknownId));

        mockMvc.perform(get("/api/menu/items/{id}", unknownId))
                .andExpect(status().isNotFound());
    }
}