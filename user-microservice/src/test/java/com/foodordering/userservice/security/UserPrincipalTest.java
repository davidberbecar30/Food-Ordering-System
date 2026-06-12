package com.foodordering.userservice.security;

import com.foodordering.userservice.entity.Role;
import com.foodordering.userservice.entity.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    void exposesAuthoritiesWithRolePrefix() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@example.com")
                .password("hash")
                .role(Role.ADMIN)
                .build();

        UserPrincipal principal = new UserPrincipal(user);

        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        assertThat(principal.getUsername()).isEqualTo("admin");
        assertThat(principal.getId()).isEqualTo(user.getId());
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
    }
}
