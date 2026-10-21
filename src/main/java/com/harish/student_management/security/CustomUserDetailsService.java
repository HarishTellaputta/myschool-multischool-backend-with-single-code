
package com.harish.student_management.security;

import com.harish.student_management.entity.User;
import com.harish.student_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsernameWithRole(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        Set<String> roleNames = user.getAllRoleNames();

        boolean isSuperAdmin = user.getAllRoleNames().stream()
                .map(String::trim)
                .anyMatch(role ->
                        role.equalsIgnoreCase("SUPER_ADMIN")
                                || role.equalsIgnoreCase("ROLE_SUPER_ADMIN"));

        boolean schoolInactive = !isSuperAdmin
                && user.getSchool() != null
                && "INACTIVE".equalsIgnoreCase(user.getSchool().getStatus());

        var authorities = roleNames.stream()
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(
                "INACTIVE".equalsIgnoreCase(user.getStatus())
                        || schoolInactive
        )
                .build();
    }
}
