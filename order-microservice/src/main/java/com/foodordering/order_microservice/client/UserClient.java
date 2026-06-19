package com.foodordering.order_microservice.client;

import com.foodordering.order_microservice.dto.TokenValidationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(@Value("${user.service.url}") String userUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(userUrl)
                .build();
    }

    public TokenValidationResponse validateToken(String authHeader) {
        return restClient.get()
                .uri("/api/users/validate-token")
                .header("Authorization", authHeader)
                .retrieve()
                .body(TokenValidationResponse.class);
    }

    @SuppressWarnings("unchecked")
    public boolean existsById(UUID userId) {
        Map<String, Object> body = restClient.get()
                .uri("/api/users/{id}/exists", userId)
                .retrieve()
                .body(Map.class);
        return body != null && Boolean.TRUE.equals(body.get("exists"));
    }
}
