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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .username("alice")
                .email("alice@example.com")
                .password("hashed-pwd")
                .role(Role.CUSTOMER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("register: creeaza un user CUSTOMER, hashuieste parola, salveaza")
    void register_success() {
        RegisterRequest req = RegisterRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("plain123")
                .build();

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain123")).thenReturn("hashed-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(userId);
            return u;
        });
        when(userMapper.toResponse(any(User.class))).thenReturn(
                UserResponse.builder().id(userId).username("alice").role(Role.CUSTOMER).build());

        UserResponse result = userService.register(req);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
        verify(passwordEncoder).encode("plain123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: arunca DuplicateResourceException daca username-ul exista")
    void register_duplicateUsername() {
        RegisterRequest req = RegisterRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("plain123")
                .build();

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: arunca DuplicateResourceException daca email-ul exista")
    void register_duplicateEmail() {
        RegisterRequest req = RegisterRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("plain123")
                .build();

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: forteaza CUSTOMER chiar daca request-ul cere ADMIN")
    void register_forceCustomerEvenIfAdminRequested() {
        RegisterRequest req = RegisterRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("plain123")
                .role(Role.ADMIN)
                .build();

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return UserResponse.builder().role(u.getRole()).build();
        });

        UserResponse result = userService.register(req);

        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("login: succes - returneaza AuthResponse cu token")
    void login_success() {
        LoginRequest req = LoginRequest.builder()
                .username("alice")
                .password("plain123")
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("plain123", "hashed-pwd")).thenReturn(true);
        when(jwtService.generateToken(sampleUser)).thenReturn("jwt.token.here");

        AuthResponse result = userService.login(req);

        assertThat(result.getToken()).isEqualTo("jwt.token.here");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("login: arunca InvalidCredentialsException daca user-ul nu exista")
    void login_userNotFound() {
        LoginRequest req = LoginRequest.builder()
                .username("ghost")
                .password("any")
                .build();

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login: arunca BadCredentialsException daca parola e gresita")
    void login_wrongPassword() {
        LoginRequest req = LoginRequest.builder()
                .username("alice")
                .password("wrong")
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong", "hashed-pwd")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("getById: returneaza user-ul mapat")
    void getById_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userMapper.toResponse(sampleUser)).thenReturn(
                UserResponse.builder().id(userId).username("alice").build());

        UserResponse result = userService.getById(userId);

        assertThat(result.getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("getById: arunca ResourceNotFoundException pentru id inexistent")
    void getById_notFound() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(missing))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateProfile: actualizeaza doar campurile furnizate")
    void updateProfile_partialUpdate() {
        UpdateProfileRequest req = UpdateProfileRequest.builder()
                .email("new@example.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return UserResponse.builder().email(u.getEmail()).username(u.getUsername()).build();
        });

        UserResponse result = userService.updateProfile(userId, req);

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getUsername()).isEqualTo("alice");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("updateProfile: hashuieste parola noua daca este furnizata")
    void updateProfile_changesPassword() {
        UpdateProfileRequest req = UpdateProfileRequest.builder()
                .password("brand-new-password")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("brand-new-password")).thenReturn("new-hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponse());

        userService.updateProfile(userId, req);

        verify(passwordEncoder).encode("brand-new-password");
        assertThat(sampleUser.getPassword()).isEqualTo("new-hashed");
    }

    @Test
    @DisplayName("updateProfile: respinge un username duplicat")
    void updateProfile_duplicateUsername() {
        UpdateProfileRequest req = UpdateProfileRequest.builder()
                .username("taken")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(userId, req))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAll: returneaza toti userii mapati")
    void getAll_success() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));
        when(userMapper.toResponse(sampleUser)).thenReturn(
                UserResponse.builder().id(userId).build());

        List<UserResponse> result = userService.getAll();

        assertThat(result).hasSize(1);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("existsById: deleaga la repository")
    void existsById_delegates() {
        when(userRepository.existsById(userId)).thenReturn(true);
        assertThat(userService.existsById(userId)).isTrue();
    }

    @Test
    @DisplayName("validateToken: token null -> invalid")
    void validateToken_nullToken() {
        TokenValidationResponse result = userService.validateToken(null);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("validateToken: token invalid -> invalid")
    void validateToken_invalidToken() {
        when(jwtService.isTokenValid("bad")).thenReturn(false);
        TokenValidationResponse result = userService.validateToken("bad");
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("validateToken: token valid + user existent -> valid cu detalii")
    void validateToken_valid() {
        when(jwtService.isTokenValid("good")).thenReturn(true);
        when(jwtService.extractUserId("good")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        TokenValidationResponse result = userService.validateToken("good");

        assertThat(result.isValid()).isTrue();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("validateToken: token valid dar user sters -> invalid")
    void validateToken_userMissing() {
        when(jwtService.isTokenValid("good")).thenReturn(true);
        when(jwtService.extractUserId("good")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        TokenValidationResponse result = userService.validateToken("good");

        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("createUser: adminul poate crea un user cu rol ADMIN")
    void createUser_adminRole_success() {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("root")
                .email("root@example.com")
                .password("plain123")
                .role(Role.ADMIN)
                .build();

        when(userRepository.existsByUsername("root")).thenReturn(false);
        when(userRepository.existsByEmail("root@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain123")).thenReturn("hashed-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(userMapper.toResponse(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return UserResponse.builder()
                    .id(u.getId()).username(u.getUsername()).role(u.getRole()).build();
        });

        UserResponse result = userService.createUser(req);

        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        assertThat(result.getUsername()).isEqualTo("root");
        verify(passwordEncoder).encode("plain123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser: adminul poate crea si un user CUSTOMER")
    void createUser_customerRole_success() {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("bob")
                .email("bob@example.com")
                .password("plain123")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return UserResponse.builder().role(u.getRole()).build();
        });

        UserResponse result = userService.createUser(req);

        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("createUser: arunca DuplicateResourceException pentru username duplicat")
    void createUser_duplicateUsername() {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("alice")
                .email("new@example.com")
                .password("plain123")
                .role(Role.ADMIN)
                .build();

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser: arunca DuplicateResourceException pentru email duplicat")
    void createUser_duplicateEmail() {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("newuser")
                .email("alice@example.com")
                .password("plain123")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser: adminul poate schimba rolul unui user din CUSTOMER in ADMIN")
    void updateUser_changeRoleToAdmin() {
        AdminUpdateUserRequest req = AdminUpdateUserRequest.builder()
                .role(Role.ADMIN)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return UserResponse.builder().id(u.getId()).role(u.getRole()).build();
        });

        UserResponse result = userService.updateUser(userId, req);

        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        assertThat(sampleUser.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("updateUser: partial update - actualizeaza doar campurile furnizate")
    void updateUser_partialUpdate() {
        AdminUpdateUserRequest req = AdminUpdateUserRequest.builder()
                .email("changed@example.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("changed@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return UserResponse.builder()
                    .username(u.getUsername()).email(u.getEmail()).role(u.getRole()).build();
        });

        UserResponse result = userService.updateUser(userId, req);

        assertThat(result.getEmail()).isEqualTo("changed@example.com");
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("updateUser: hashuieste parola noua daca este furnizata")
    void updateUser_changesPassword() {
        AdminUpdateUserRequest req = AdminUpdateUserRequest.builder()
                .password("new-secret-pass")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("new-secret-pass")).thenReturn("new-hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponse());

        userService.updateUser(userId, req);

        verify(passwordEncoder).encode("new-secret-pass");
        assertThat(sampleUser.getPassword()).isEqualTo("new-hashed");
    }

    @Test
    @DisplayName("updateUser: respinge un username duplicat")
    void updateUser_duplicateUsername() {
        AdminUpdateUserRequest req = AdminUpdateUserRequest.builder()
                .username("taken")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(userId, req))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser: arunca ResourceNotFoundException pentru id inexistent")
    void updateUser_notFound() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(missing,
                AdminUpdateUserRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteUser: sterge user-ul daca exista")
    void deleteUser_success() {
        UUID adminId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        userService.deleteUser(userId, adminId);

        verify(userRepository).delete(sampleUser);
    }

    @Test
    @DisplayName("deleteUser: adminul NU isi poate sterge propriul cont")
    void deleteUser_selfDeleteForbidden() {
        UUID adminId = UUID.randomUUID();

        assertThatThrownBy(() -> userService.deleteUser(adminId, adminId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot delete their own account");

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("deleteUser: arunca ResourceNotFoundException pentru id inexistent")
    void deleteUser_notFound() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(missing, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("getAllByRole: returneaza doar userii cu rolul cerut")
    void getAllByRole_filtersCorrectly() {
        User admin = User.builder()
                .id(UUID.randomUUID()).username("root").email("root@example.com")
                .password("hash").role(Role.ADMIN).build();

        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));
        when(userMapper.toResponse(admin)).thenReturn(
                UserResponse.builder().username("root").role(Role.ADMIN).build());

        List<UserResponse> result = userService.getAllByRole(Role.ADMIN);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository).findByRole(Role.ADMIN);
        verify(userRepository, never()).findAll();
    }
}
