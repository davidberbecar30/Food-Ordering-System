package com.foodordering.userservice.controller;

import com.foodordering.userservice.dto.request.LoginRequest;
import com.foodordering.userservice.dto.request.RegisterRequest;
import com.foodordering.userservice.dto.request.UpdateProfileRequest;
import com.foodordering.userservice.dto.response.AuthResponse;
import com.foodordering.userservice.dto.response.ExistsResponse;
import com.foodordering.userservice.dto.response.TokenValidationResponse;
import com.foodordering.userservice.dto.response.UserResponse;
import com.foodordering.userservice.security.UserPrincipal;
import com.foodordering.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Service", description = "Authentication and user profile endpoints")
public class UserController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Login and obtain a JWT token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @Operation(summary = "Get current user profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getById(principal.getId()));
    }

    @Operation(summary = "Update current user profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), request));
    }

    @Operation(summary = "List all users (ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userService.getAll());
    }

    @Operation(summary = "Internal: check whether a user exists by id")
    @GetMapping("/{id}/exists")
    public ResponseEntity<ExistsResponse> existsById(@PathVariable UUID id) {
        return ResponseEntity.ok(ExistsResponse.builder()
                .exists(userService.existsById(id))
                .build());
    }

    @Operation(summary = "Internal: validate a JWT and return user info")
    @GetMapping("/{id}/validate-token")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = null;
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length());
        }

        TokenValidationResponse response = userService.validateToken(token);

        if (response.isValid() && !id.equals(response.getUserId())) {
            return ResponseEntity.ok(TokenValidationResponse.builder().valid(false).build());
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Internal: validate a JWT without a known user id")
    @GetMapping("/validate-token")
    public ResponseEntity<TokenValidationResponse> validateTokenOnly(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = null;
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length());
        }

        return ResponseEntity.ok(userService.validateToken(token));
    }
}
