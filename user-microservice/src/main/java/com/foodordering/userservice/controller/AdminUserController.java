package com.foodordering.userservice.controller;

import com.foodordering.userservice.dto.request.AdminCreateUserRequest;
import com.foodordering.userservice.dto.request.AdminUpdateUserRequest;
import com.foodordering.userservice.dto.response.UserResponse;
import com.foodordering.userservice.entity.Role;
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
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - User Management",
        description = "Full CRUD over users of any role (ADMIN only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final UserService userService;

    @Operation(summary = "Create a user with any role (CUSTOMER or ADMIN)")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody AdminCreateUserRequest request) {
        UserResponse created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "List all users, optionally filtered by role")
    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(
            @RequestParam(name = "role", required = false) Role role) {
        if (role != null) {
            return ResponseEntity.ok(userService.getAllByRole(role));
        }
        return ResponseEntity.ok(userService.getAll());
    }

    @Operation(summary = "Get a user by id")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @Operation(summary = "Update any user (username, email, password, role)")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @Operation(summary = "Delete a user (admins cannot delete their own account)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteUser(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
