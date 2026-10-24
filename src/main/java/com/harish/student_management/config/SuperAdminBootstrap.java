
package com.harish.student_management.config;

import com.harish.student_management.entity.Role;
import com.harish.student_management.entity.User;
import com.harish.student_management.repository.RoleRepository;
import com.harish.student_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${SUPER_ADMIN_ENABLED:false}")
    private boolean enabled;

    @Value("${SUPER_ADMIN_USERNAME:}")
    private String username;

    @Value("${SUPER_ADMIN_EMAIL:}")
    private String email;

    @Value("${SUPER_ADMIN_PASSWORD:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        // Disabled by default. Runs only when explicitly enabled.
        if (!enabled) {
            return;
        }

        if (username == null || username.isBlank()
                || email == null || email.isBlank()
                || password == null || password.length() < 12) {
            throw new IllegalStateException(
                    "Super Admin bootstrap requires username, email, "
                            + "and a password of at least 12 characters."
            );
        }

        // Do not create another primary Super Admin.
        if (userRepository.existsByRole_Name("SUPER_ADMIN")) {
            System.out.println(
                    "Super Admin already exists. Bootstrap skipped."
            );
            return;
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException(
                    "Username already exists. Choose another username."
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException(
                    "Email already exists. Choose another email."
            );
        }

        Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name("SUPER_ADMIN")
                                .description(
                                        "Platform-level Super Administrator"
                                )
                                .build()
                ));

        User superAdmin = User.builder()
                .username(username.trim())
                .email(email.trim())
                .password(passwordEncoder.encode(password))
                .role(superAdminRole)
                .school(null)
                .status("ACTIVE")
                .build();

        userRepository.save(superAdmin);

        System.out.println(
                "Super Admin account created successfully."
        );
    }
}
