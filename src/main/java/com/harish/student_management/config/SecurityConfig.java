package com.harish.student_management.config;

import com.harish.student_management.security.CustomUserDetailsService;
import com.harish.student_management.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .cors(cors -> {
                })

                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        ))

                .authorizeHttpRequests(auth -> auth

                                // IMPORTANT:
                                // Allow browser CORS preflight requests
                                .requestMatchers(
                                        org.springframework.http.HttpMethod.OPTIONS,
                                        "/**"
                                ).permitAll()

                                // Public endpoints
                                .requestMatchers(
                                        "/auth/register",
                                        "/auth/login",
                                        "/auth/forgot-password",
                                        "/auth/reset-password",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/actuator/health",
                                        "/actuator/info"
                                ).permitAll()

                                .requestMatchers(
                                        "/auth/change-password",
                                        "/auth/logout"
                                ).authenticated()

                                .requestMatchers("/actuator/**")
                                .hasRole("SCHOOL_ADMIN")

                                // School Admin endpoints
                                .requestMatchers("/api/v1/schools/**")
                                .hasRole("SCHOOL_ADMIN")

                                .requestMatchers("/api/v1/roles/**")
                                .hasRole("SCHOOL_ADMIN")

                                .requestMatchers("/api/v1/classes/**")
                                .hasAnyRole("SCHOOL_ADMIN", "TEACHER")

                                .requestMatchers("/api/v1/sections/**")
                                .hasAnyRole("SCHOOL_ADMIN", "TEACHER")

                                .requestMatchers("/api/v1/academic-years/**")
                                .hasRole("SCHOOL_ADMIN")

                                .requestMatchers("/api/v1/examinations/**")
                                .hasAnyRole(
                                        "SCHOOL_ADMIN",
                                        "TEACHER",
                                        "PARENT",
                                        "STUDENT"
                                )

                                .requestMatchers("/api/v1/mcq-questions/**")
                                .hasAnyRole("SCHOOL_ADMIN", "TEACHER")

                                .requestMatchers("/api/v1/mcq-tests/**")
                                .hasAnyRole(
                                        "SCHOOL_ADMIN",
                                        "TEACHER",
                                        "PARENT",
                                        "STUDENT"
                                )

                                .requestMatchers("/api/v1/fees/**")
                                .hasAnyRole("SCHOOL_ADMIN", "PARENT")

                                .requestMatchers("/api/v1/teacher-timetables/**")
                                .hasAnyRole(
                                        "SCHOOL_ADMIN",
                                        "TEACHER",
                                        "PARENT",
                                        "STUDENT"
                                )

                                .requestMatchers("/api/v1/calendar-events/**")
                                .hasAnyRole(
                                        "SCHOOL_ADMIN",
                                        "PARENT",
                                        "TEACHER",
                                        "STUDENT"
                                )

                                .requestMatchers("/api/v1/notifications/**")
                                .authenticated()

                                .requestMatchers("/api/v1/notices/**")
                                .hasAnyRole(
                                        "SCHOOL_ADMIN",
                                        "PARENT",
                                        "TEACHER",
                                        "STUDENT"
                                )

                                .requestMatchers("/api/v1/transport/**")
                                .hasAnyRole(
                                        "SCHOOL_ADMIN",
                                        "PARENT",
                                        "TEACHER",
                                        "STUDENT"
                                )

                                .requestMatchers("/api/v1/transport/tracking/**")
                                .hasAnyRole(
                                        "SCHOOL_ADMIN",
                                        "PARENT",
                                        "TEACHER",
                                        "STUDENT"
                                )

                                .requestMatchers("/api/v1/communications/**")
                                .hasAnyRole("PARENT", "TEACHER")

                                .requestMatchers("/api/v1/certificates/**")
                                .hasRole("SCHOOL_ADMIN")

                                .requestMatchers("/api/v1/achievements/**")
                                .hasAnyRole(
                                        "SCHOOL_ADMIN",
                                        "TEACHER",
                                        "PARENT",
                                        "STUDENT"
                                )

                                .requestMatchers("/api/v1/admin/dashboard/**")
                                .hasRole("SCHOOL_ADMIN")

                                .requestMatchers("/api/v1/reports/**")
                                .hasAnyRole(
                                        "SCHOOL_ADMIN",
                                        "TEACHER",
                                        "PARENT",
                                        "STUDENT"
                                )

                                .requestMatchers("/api/v1/journey-timeline/**")
                                .hasAnyRole("PARENT", "SCHOOL_ADMIN")

                                .requestMatchers("/api/v1/teacher-events/**")
                                .hasRole("TEACHER")

                                // Teacher endpoints
                                .requestMatchers("/api/v1/teachers/**")
                                .hasAnyRole("SCHOOL_ADMIN", "TEACHER")

                                .requestMatchers("/api/v1/teacher-class-assignments/**")
                                .hasAnyRole("SCHOOL_ADMIN", "TEACHER")

                                .requestMatchers("/api/v1/teacher-leaves/**")
                                .hasAnyRole("SCHOOL_ADMIN", "TEACHER")

                                // Attendance
                                .requestMatchers("/api/v1/attendances/**")
                                .hasAnyRole(
                                        "SCHOOL_ADMIN",
                                        "TEACHER",
                                        "PARENT",
                                        "STUDENT"
                                )

                                // Homework
                                .requestMatchers("/api/v1/homeworks/**")
                                .hasAnyRole(
                                        "SCHOOL_ADMIN",
                                        "TEACHER",
                                        "PARENT",
                                        "STUDENT"
                                )

                                // Assignment
                                .requestMatchers("/api/v1/assignments/**")
                                .hasAnyRole(
                                        "SCHOOL_ADMIN",
                                        "TEACHER",
                                        "PARENT",
                                        "STUDENT"
                                )

                                // Student
                                .requestMatchers("/api/v1/students/**")
                                .hasAnyRole(
                                        "SCHOOL_ADMIN",
                                        "TEACHER",
                                        "PARENT",
                                        "STUDENT"
                                )
                                // Birthday list - all authenticated users
                                .requestMatchers("/api/v1/student-birthday-status/chat")
                                .authenticated()

// Birthday - today's birthdays
                                .requestMatchers("/api/v1/student-birthday-status/today")
                                .authenticated()

// Birthday - chat
                                .requestMatchers("/api/v1/student-birthday-status/chat")
                                .authenticated()

// Birthday status management
                                .requestMatchers("/api/v1/student-birthday-status/**")
                                .authenticated()
                                .requestMatchers("/api/v1/parents/**")
                                .hasAnyRole("SCHOOL_ADMIN", "TEACHER", "PARENT")
                                .requestMatchers("/api/v1/files/**")
                                .authenticated()

                                .requestMatchers("/api/v1/subjects/**")
                                .hasAnyRole("SCHOOL_ADMIN", "TEACHER")

                                // Everything else
                                .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}