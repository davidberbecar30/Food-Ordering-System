package com.foodordering.userservice.service;

import com.foodordering.userservice.dto.request.AdminCreateUserRequest;
import com.foodordering.userservice.dto.request.AdminUpdateUserRequest;
import com.foodordering.userservice.dto.request.LoginRequest;
import com.foodordering.userservice.dto.request.RegisterRequest;
import com.foodordering.userservice.dto.request.UpdateProfileRequest;
import com.foodordering.userservice.dto.response.AuthResponse;
import com.foodordering.userservice.dto.response.TokenValidationResponse;
import com.foodordering.userservice.dto.response.UserResponse;
import com.foodordering.userservice.entity.Role;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getById(UUID id);

    UserResponse updateProfile(UUID currentUserId, UpdateProfileRequest request);

    List<UserResponse> getAll();

    boolean existsById(UUID id);

    TokenValidationResponse validateToken(String token);

    UserResponse createUser(AdminCreateUserRequest request);

    UserResponse updateUser(UUID id, AdminUpdateUserRequest request);

    void deleteUser(UUID id, UUID currentAdminId);

    List<UserResponse> getAllByRole(Role role);
}
