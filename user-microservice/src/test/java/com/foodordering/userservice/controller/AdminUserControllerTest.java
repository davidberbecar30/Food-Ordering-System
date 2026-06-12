package com.foodordering.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodordering.userservice.config.SecurityConfig;
import com.foodordering.userservice.dto.request.AdminCreateUserRequest;
import com.foodordering.userservice.dto.request.AdminUpdateUserRequest;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
class AdminUserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService userDetailsService;

    private UUID targetUserId;
    private UserResponse customerResponse;
    private UserPrincipal adminPrincipal;
    private UserPrincipal customerPrincipal;

    @BeforeEach
    void setUp() {
        targetUserId = UUID.randomUUID();

        customerResponse = UserResponse.builder()
                .id(targetUserId)
                .username("alice")
                .email("alice@example.com")
                .role(Role.CUSTOMER)
                .createdAt(LocalDateTime.now())
                .build();

        adminPrincipal = new UserPrincipal(User.builder()
                .id(UUID.randomUUID()).username("root").email("root@example.com")
                .password("hash").role(Role.ADMIN).build());

        customerPrincipal = new UserPrincipal(User.builder()
                .id(UUID.randomUUID()).username("alice").email("alice@example.com")
                .password("hash").role(Role.CUSTOMER).build());
    }

    @Test
    @DisplayName("POST: adminul poate crea un user cu rol ADMIN -> 201")
    void createUser_asAdmin_returns201() throws Exception {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("newadmin")
                .email("newadmin@example.com")
                .password("plain123")
                .role(Role.ADMIN)
                .build();

        UserResponse created = UserResponse.builder()
                .id(UUID.randomUUID()).username("newadmin")
                .email("newadmin@example.com").role(Role.ADMIN).build();

        when(userService.createUser(any())).thenReturn(created);

        mockMvc.perform(post("/api/admin/users")
                        .with(user(adminPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newadmin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST: un CUSTOMER nu poate crea useri -> 403")
    void createUser_asCustomer_returns403() throws Exception {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("hacker")
                .email("hacker@example.com")
                .password("plain123")
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(post("/api/admin/users")
                        .with(user(customerPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST: fara autentificare -> 401")
    void createUser_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST: request invalid (fara rol) -> 400")
    void createUser_missingRole_returns400() throws Exception {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("norole")
                .email("norole@example.com")
                .password("plain123")
                .build();

        mockMvc.perform(post("/api/admin/users")
                        .with(user(adminPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET: adminul listeaza toti userii -> 200")
    void listUsers_asAdmin_returns200() throws Exception {
        when(userService.getAll()).thenReturn(List.of(customerResponse));

        mockMvc.perform(get("/api/admin/users")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));

        verify(userService).getAll();
    }

    @Test
    @DisplayName("GET: adminul poate filtra userii dupa rol -> 200")
    void listUsers_filteredByRole_returns200() throws Exception {
        UserResponse adminUser = UserResponse.builder()
                .id(UUID.randomUUID()).username("root").role(Role.ADMIN).build();
        when(userService.getAllByRole(Role.ADMIN)).thenReturn(List.of(adminUser));

        mockMvc.perform(get("/api/admin/users")
                        .param("role", "ADMIN")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("ADMIN"));

        verify(userService).getAllByRole(Role.ADMIN);
    }

    @Test
    @DisplayName("GET: un CUSTOMER nu poate lista userii -> 403")
    void listUsers_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(user(customerPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET by id: adminul poate citi orice user -> 200")
    void getUser_asAdmin_returns200() throws Exception {
        when(userService.getById(targetUserId)).thenReturn(customerResponse);

        mockMvc.perform(get("/api/admin/users/" + targetUserId)
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUserId.toString()))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @DisplayName("PUT: adminul poate schimba rolul unui user -> 200")
    void updateUser_asAdmin_returns200() throws Exception {
        AdminUpdateUserRequest req = AdminUpdateUserRequest.builder()
                .role(Role.ADMIN)
                .build();

        UserResponse promoted = UserResponse.builder()
                .id(targetUserId).username("alice").role(Role.ADMIN).build();

        when(userService.updateUser(eq(targetUserId), any())).thenReturn(promoted);

        mockMvc.perform(put("/api/admin/users/" + targetUserId)
                        .with(user(adminPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        ArgumentCaptor<AdminUpdateUserRequest> captor =
                ArgumentCaptor.forClass(AdminUpdateUserRequest.class);
        verify(userService).updateUser(eq(targetUserId), captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("PUT: un CUSTOMER nu poate actualiza useri -> 403")
    void updateUser_asCustomer_returns403() throws Exception {
        mockMvc.perform(put("/api/admin/users/" + targetUserId)
                        .with(user(customerPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE: adminul poate sterge un user -> 204")
    void deleteUser_asAdmin_returns204() throws Exception {
        doNothing().when(userService).deleteUser(eq(targetUserId), any());

        mockMvc.perform(delete("/api/admin/users/" + targetUserId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(eq(targetUserId), eq(adminPrincipal.getId()));
    }

    @Test
    @DisplayName("DELETE: un CUSTOMER nu poate sterge useri -> 403")
    void deleteUser_asCustomer_returns403() throws Exception {
        mockMvc.perform(delete("/api/admin/users/" + targetUserId)
                        .with(user(customerPrincipal))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
