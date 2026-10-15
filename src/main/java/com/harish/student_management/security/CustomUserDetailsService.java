package com.harish.student_management.security;

import com.harish.student_management.entity.User;
import com.harish.student_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsernameWithRole(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        String role = user.getRole() != null
                ? user.getRole().getName()
                : "STUDENT";

        System.out.println("=================================");
        System.out.println("LOGIN USERNAME : " + user.getUsername());
        System.out.println("DB ROLE        : " + role);
        System.out.println("SPRING ROLE     : ROLE_" + role);
        System.out.println("=================================");

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(role)   // IMPORTANT: no ROLE_ here
                .disabled("INACTIVE".equalsIgnoreCase(user.getStatus()))
                .build();
    }
}