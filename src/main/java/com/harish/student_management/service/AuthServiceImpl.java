package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.Role;
import com.harish.student_management.entity.Teacher;
import com.harish.student_management.entity.User;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.exception.UserAlreadyExistsException;
import com.harish.student_management.repository.TeacherRepository;
import com.harish.student_management.repository.UserRepository;
import com.harish.student_management.repository.RoleRepository;
import com.harish.student_management.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TeacherRepository teacherRepository;

    @Override
    public UserResponseDto register(UserRequestDto request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        // Find requested role
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role not found: " + request.getRole()
                        ));

        // Restrict to only one SCHOOL_ADMIN
        if ("SCHOOL_ADMIN".equalsIgnoreCase(role.getName())
                && userRepository.existsByRole_Name("SCHOOL_ADMIN")) {

            throw new UserAlreadyExistsException(
                    "Only one SCHOOL_ADMIN is allowed"
            );
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status("ACTIVE")
                .build();

        User savedUser = userRepository.save(user);

        logger.info(
                "User registered successfully with username: {} and role: {}",
                savedUser.getUsername(),
                savedUser.getRole()
        );

        return UserResponseDto.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole() == null
                        ? null
                        : savedUser.getRole().getName())
                .build();
    }
    @Override
    public LoginResponseDto login(LoginRequestDto request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Block inactive teacher accounts
        if (user.getRole() != null
                && "TEACHER".equalsIgnoreCase(user.getRole().getName())) {

            Teacher teacher = teacherRepository.findByUserId(user.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Teacher profile not found"
                            ));

            if (!"ACTIVE".equalsIgnoreCase(teacher.getStatus())) {
                throw new IllegalArgumentException(
                        "Your teacher account is inactive. Please contact the school administrator."
                );
            }
        }

        String token = jwtService.generateToken(request.getUsername());

        logger.info(
                "User logged in successfully with username: {} and id: {}",
                user.getUsername(),
                user.getId()
        );

        return LoginResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(
                        user.getRole() == null
                                ? null
                                : user.getRole().getName()
                )
                .build();
    }
    @Override
    public String forgotPassword(ForgotPasswordRequestDto request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with email: " + request.getEmail()
                        ));

        // Generate ONE reset token
        String resetToken = UUID.randomUUID().toString();

        // Store only the hash in database
        user.setPasswordResetTokenHash(hashToken(resetToken));

        // Token valid for 30 minutes
        user.setPasswordResetExpiresAt(
                LocalDateTime.now().plusMinutes(30)
        );

        userRepository.save(user);

        logger.info(
                "Password reset token generated for user: {}",
                user.getUsername()
        );

        // Temporary response for development/testing
        return "Password reset token generated: " + resetToken;
    }

    @Override
    public String resetPassword(ResetPasswordRequestDto request) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        String tokenHash = hashToken(request.getToken());

        User user = userRepository
                .findByPasswordResetTokenHash(tokenHash)
                .filter(u ->
                        u.getPasswordResetExpiresAt() != null
                                && u.getPasswordResetExpiresAt()
                                .isAfter(LocalDateTime.now())
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invalid or expired token"
                        ));

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        // Make token single-use
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);

        userRepository.save(user);

        logger.info(
                "Password reset successful for user: {}",
                user.getUsername()
        );

        return "Password reset successful";
    }
    @Override
    public String changePassword(ChangePasswordRequestDto request, String username) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        logger.info("Password changed successfully for user: {}", user.getUsername());

        return "Password changed successfully";
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    @Override
    public LogoutResponseDto logout(String token) {
        logger.info("User logged out successfully");

        return LogoutResponseDto.builder()
                .message("Logout successful")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
