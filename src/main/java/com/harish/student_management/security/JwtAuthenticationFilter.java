package com.harish.student_management.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import io.jsonwebtoken.JwtException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("=================================");
        System.out.println("REQUEST URI : " + request.getRequestURI());
        System.out.println("METHOD      : " + request.getMethod());
        System.out.println("=================================");

        // ---------------------------------------------------------
        // IMPORTANT:
        // Browser sends OPTIONS before requests such as:
        // POST /api/v1/students/import-excel
        //
        // Do NOT process JWT for CORS preflight requests.
        // ---------------------------------------------------------
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {

            System.out.println("CORS PREFLIGHT REQUEST - BYPASSING JWT");

            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        // No Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        String username;

        try {

            username = jwtService.extractUsername(token);

        } catch (JwtException | IllegalArgumentException ex) {

            System.out.println("JWT EXTRACTION FAILED");

            filterChain.doFilter(request, response);
            return;
        }

        if (username != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            try {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);

                boolean tokenValid =
                        jwtService.isTokenValid(token, userDetails);

                System.out.println("=================================");
                System.out.println("JWT USERNAME : " + username);
                System.out.println("DB USERNAME  : " + userDetails.getUsername());
                System.out.println("AUTHORITIES  : " + userDetails.getAuthorities());
                System.out.println("TOKEN VALID  : " + tokenValid);
                System.out.println("=================================");

                if (tokenValid) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }

            } catch (JwtException | IllegalArgumentException ex) {

                System.out.println("JWT VALIDATION FAILED");

                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}