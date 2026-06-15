package com.foodordering.order_microservice.client;

import com.foodordering.order_microservice.dto.MenuItemDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MenuClient {

    private final RestClient restClient;

    public MenuClient(
            @Value("${menu.service.url}") String menuUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(menuUrl)
                .build();
    }

    public MenuItemDto getMenuItem(Long id) {

        return restClient.get()
                .uri("/menu-items/{id}", id)
                .retrieve()
                .body(MenuItemDto.class);
    }
}