
package com.harish.student_management.config;

import com.harish.student_management.entity.Role;
import com.harish.student_management.entity.User;
import com.harish.student_management.repository.RoleRepository;
import com.harish.student_management.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class RoleInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Bean
    public ApplicationRunner initializeRolesAndSuperAdmin() {
        return args -> {

            Map<String, String> roles = Map.of(
                    "SUPER_ADMIN", "Platform Super Administrator",
                    "SCHOOL_ADMIN", "School Administrator",
                    "TEACHER", "Teacher",
                    "PARENT", "Parent",
                    "STUDENT", "Student"
            );

            // 1. Ensure all required roles exist.
            for (Map.Entry<String, String> entry : roles.entrySet()) {
                if (!roleRepository.existsByName(entry.getKey())) {
                    roleRepository.save(
                            Role.builder()
                                    .name(entry.getKey())
                                    .description(entry.getValue())
                                    .build()
                    );
                }
            }

            // 2. Read initial Super Admin credentials from environment.
            String username =
                    environment.getProperty("SUPER_ADMIN_USERNAME");
            String email =
                    environment.getProperty("SUPER_ADMIN_EMAIL");
            String password =
                    environment.getProperty("SUPER_ADMIN_PASSWORD");

            // Do not create an account without explicitly configured credentials.
            if (username == null || username.isBlank()
                    || email == null || email.isBlank()
                    || password == null || password.length() < 12) {

                System.out.println(
                        "Super Admin bootstrap skipped: configure "
                                + "SUPER_ADMIN_USERNAME, SUPER_ADMIN_EMAIL "
                                + "and SUPER_ADMIN_PASSWORD."
                );
                return;
            }

            // 3. Never create another Super Admin if one already exists.
            if (userRepository.existsByRole_Name("SUPER_ADMIN")) {
                System.out.println(
                        "Super Admin already exists; bootstrap skipped."
                );
                return;
            }

            // 4. Prevent collisions with existing accounts.
            if (userRepository.existsByUsername(username)
                    || userRepository.existsByEmail(email)) {
                throw new IllegalStateException(
                        "Super Admin bootstrap username or email "
                                + "already belongs to another account."
                );
            }

            Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "SUPER_ADMIN role was not initialized."
                            ));

            // 5. Create the initial account with an encoded password.
            User superAdmin = User.builder()
                    .username(username.trim())
                    .email(email.trim())
                    .password(passwordEncoder.encode(password))
                    .role(superAdminRole)
                    .status("ACTIVE")
                    .build();

            userRepository.save(superAdmin);

            System.out.println(
                    "Initial Super Admin account created successfully."
            );
        };
    }
}
