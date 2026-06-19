package com.foodordering.order_microservice.security;

import com.foodordering.order_microservice.client.UserClient;
import com.foodordering.order_microservice.dto.TokenValidationResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private static final List<String> SKIP_PREFIXES = List.of(
            "/actuator", "/swagger-ui", "/v3/api-docs", "/swagger-ui.html", "/api-docs"
    );

    private final UserClient userClient;

    public AuthFilter(UserClient userClient) {
        this.userClient = userClient;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        TokenValidationResponse validation;
        try {
            validation = userClient.validateToken(authHeader);
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Unable to reach user service");
            return;
        }

        if (validation == null || !validation.valid()) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        request.setAttribute("authenticatedUser", validation);
        chain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
