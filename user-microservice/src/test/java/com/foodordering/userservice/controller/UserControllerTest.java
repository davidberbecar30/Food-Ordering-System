package com.foodordering.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodordering.userservice.dto.request.LoginRequest;
import com.foodordering.userservice.dto.request.RegisterRequest;
import com.foodordering.userservice.dto.response.AuthResponse;
import com.foodordering.userservice.dto.response.UserResponse;
import com.foodordering.userservice.entity.Role;
import com.foodordering.userservice.entity.User;
import com.foodordering.userservice.security.CustomUserDetailsService;
import com.foodordering.userservice.security.JwtAuthenticationEntryPoint;
import com.foodordering.userservice.security.JwtAuthenticationFilter;
import com.foodordering.userservice.security.JwtService;
import com.foodordering.userservice.security.UserPrincipal;
import com.foodordering.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({com.foodordering.userservice.config.SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class})
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService userDetailsService;

    private UUID userId;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userResponse = UserResponse.builder()
                .id(userId)
                .username("alice")
                .email("alice@example.com")
                .role(Role.CUSTOMER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void register_returns201AndUserBody() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("plain123")
                .build();
        when(userService.register(any())).thenReturn(userResponse);

        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_returns400ForMissingFields() throws Exception {
        RegisterRequest req = RegisterRequest.builder().build();

        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns200AndAuthResponse() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .username("alice")
                .password("plain123")
                .build();
        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt.token.here")
                .tokenType("Bearer")
                .userId(userId)
                .username("alice")
                .role(Role.CUSTOMER)
                .build();
        when(userService.login(any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void me_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withCustomerAuth_returns200() throws Exception {
        User entity = User.builder()
                .id(userId).username("alice").email("alice@example.com")
                .password("hash").role(Role.CUSTOMER).build();
        UserPrincipal principal = new UserPrincipal(entity);

        when(userService.getById(any())).thenReturn(userResponse);

        mockMvc.perform(get("/api/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void listUsers_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_asAdmin_returns200() throws Exception {
        User entity = User.builder()
                .id(userId).username("root").email("root@example.com")
                .password("hash").role(Role.ADMIN).build();
        UserPrincipal principal = new UserPrincipal(entity);

        when(userService.getAll()).thenReturn(List.of(userResponse));

        mockMvc.perform(get("/api/users")
                        .with(SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    void existsById_isPublicForInternalCalls() throws Exception {
        when(userService.existsById(eq(userId))).thenReturn(true);

        mockMvc.perform(get("/api/users/" + userId + "/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }
}
