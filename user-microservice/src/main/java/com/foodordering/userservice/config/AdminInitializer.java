package com.foodordering.userservice.config;

import com.foodordering.userservice.entity.Role;
import com.foodordering.userservice.entity.User;
import com.foodordering.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer {

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.email:admin@foodordering.local}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Bean
    public CommandLineRunner createDefaultAdmin(UserRepository userRepository,
                                                PasswordEncoder passwordEncoder) {
        return args -> {
            boolean adminExists = !userRepository.findByRole(Role.ADMIN).isEmpty();
            if (adminExists) {
                log.info("Admin account already exists - skipping default admin creation");
                return;
            }
            if (userRepository.existsByUsername(adminUsername)
                    || userRepository.existsByEmail(adminEmail)) {
                log.warn("Default admin username/email already taken by a non-admin user - skipping");
                return;
            }

            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(admin);
            log.info("Created default ADMIN account: username='{}' " +
                    "(change the password immediately in production!)", adminUsername);
        };
    }
}
