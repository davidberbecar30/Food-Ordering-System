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
import com.foodordering.userservice.entity.User;
import com.foodordering.userservice.exception.DuplicateResourceException;
import com.foodordering.userservice.exception.InvalidCredentialsException;
import com.foodordering.userservice.exception.ResourceNotFoundException;
import com.foodordering.userservice.mapper.UserMapper;
import com.foodordering.userservice.repository.UserRepository;
import com.foodordering.userservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException(
                    "Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Email already registered: " + request.getEmail());
        }

        Role roleToAssign = (request.getRole() == Role.ADMIN || request.getRole() == null)
                ? Role.CUSTOMER
                : request.getRole();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(roleToAssign)
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user id={} username={}", saved.getId(), saved.getUsername());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);
        log.info("User logged in: id={} username={}", user.getId(), user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID currentUserId, UpdateProfileRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + currentUserId));

        if (StringUtils.hasText(request.getUsername())
                && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new DuplicateResourceException(
                        "Username already taken: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        if (StringUtils.hasText(request.getEmail())
                && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException(
                        "Email already registered: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updated = userRepository.save(user);
        log.info("Updated profile for user id={}", updated.getId());
        return userMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return userRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenValidationResponse validateToken(String token) {
        if (token == null || !jwtService.isTokenValid(token)) {
            return TokenValidationResponse.builder().valid(false).build();
        }

        try {
            UUID userId = jwtService.extractUserId(token);
            return userRepository.findById(userId)
                    .map(user -> TokenValidationResponse.builder()
                            .valid(true)
                            .userId(user.getId())
                            .username(user.getUsername())
                            .role(user.getRole())
                            .build())
                    .orElseGet(() -> TokenValidationResponse.builder().valid(false).build());
        } catch (Exception ex) {
            log.debug("Token validation failed: {}", ex.getMessage());
            return TokenValidationResponse.builder().valid(false).build();
        }
    }

    @Override
    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException(
                    "Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        User saved = userRepository.save(user);
        log.info("Admin created user id={} username={} role={}",
                saved.getId(), saved.getUsername(), saved.getRole());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, AdminUpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id));

        if (StringUtils.hasText(request.getUsername())
                && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new DuplicateResourceException(
                        "Username already taken: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        if (StringUtils.hasText(request.getEmail())
                && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException(
                        "Email already registered: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        User updated = userRepository.save(user);
        log.info("Admin updated user id={} role={}", updated.getId(), updated.getRole());
        return userMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id, UUID currentAdminId) {
        if (id.equals(currentAdminId)) {
            throw new IllegalArgumentException(
                    "Admins cannot delete their own account");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id));

        userRepository.delete(user);
        log.info("Admin deleted user id={} username={}", user.getId(), user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllByRole(Role role) {
        return userRepository.findByRole(role).stream()
                .map(userMapper::toResponse)
                .toList();
    }
}
