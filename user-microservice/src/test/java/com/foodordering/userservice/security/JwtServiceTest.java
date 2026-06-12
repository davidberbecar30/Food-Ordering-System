package com.foodordering.userservice.security;

import com.foodordering.userservice.entity.Role;
import com.foodordering.userservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "test-secret-key-for-tests-only-must-be-at-least-32-bytes-long");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);
        jwtService.init();

        user = User.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .email("alice@example.com")
                .password("hashed")
                .role(Role.CUSTOMER)
                .build();
    }

    @Test
    @DisplayName("Genereaza un token nevid pentru un user")
    void generateToken_notEmpty() {
        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Token-ul generat este considerat valid")
    void freshTokenIsValid() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("Extragerea claim-urilor functioneaza corect")
    void extractClaims_correct() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.extractRole(token)).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("Un token semnat cu alta cheie este invalid")
    void tokenSignedWithDifferentKey_isInvalid() {
        String token = jwtService.generateToken(user);

        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "secret",
                "completely-different-secret-key-also-32-bytes-long-or-more");
        ReflectionTestUtils.setField(otherService, "expirationMs", 3600000L);
        otherService.init();

        assertThat(otherService.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("Un token expirat este invalid")
    void expiredToken_isInvalid() {
        JwtService shortLived = new JwtService();
        ReflectionTestUtils.setField(shortLived, "secret",
                "test-secret-key-for-tests-only-must-be-at-least-32-bytes-long");
        ReflectionTestUtils.setField(shortLived, "expirationMs", -1000L);
        shortLived.init();

        String token = shortLived.generateToken(user);
        assertThat(shortLived.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("Un token complet aiurea este invalid")
    void garbageString_isInvalid() {
        assertThat(jwtService.isTokenValid("not-a-jwt")).isFalse();
        assertThat(jwtService.isTokenValid("")).isFalse();
    }

    @Test
    @DisplayName("init() arunca exceptie pentru secret prea scurt")
    void shortSecret_throwsOnInit() {
        JwtService weak = new JwtService();
        ReflectionTestUtils.setField(weak, "secret", "short");
        ReflectionTestUtils.setField(weak, "expirationMs", 3600000L);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, weak::init);
    }
}
