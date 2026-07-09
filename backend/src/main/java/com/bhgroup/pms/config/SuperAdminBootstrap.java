package com.bhgroup.pms.config;

import com.bhgroup.pms.user.Role;
import com.bhgroup.pms.user.User;
import com.bhgroup.pms.user.UserRepository;
import com.bhgroup.pms.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first SUPER_ADMIN account on startup when none exists yet.
 * Self-service registration only ever creates GUEST accounts, so this is the
 * sole entry point to bootstrap platform administration for a fresh environment.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${SUPER_ADMIN_EMAIL:}")
    private String superAdminEmail;

    @Value("${SUPER_ADMIN_PASSWORD:}")
    private String superAdminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (superAdminEmail.isBlank() || superAdminPassword.isBlank()) {
            return;
        }

        if (userRepository.countByRole(Role.SUPER_ADMIN) > 0) {
            return;
        }

        User superAdmin = User.builder()
                .email(superAdminEmail.toLowerCase())
                .passwordHash(passwordEncoder.encode(superAdminPassword))
                .firstName("Super")
                .lastName("Admin")
                .role(Role.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        userRepository.save(superAdmin);

        log.info("Bootstrapped initial SUPER_ADMIN account for {}", superAdminEmail);
    }
}
