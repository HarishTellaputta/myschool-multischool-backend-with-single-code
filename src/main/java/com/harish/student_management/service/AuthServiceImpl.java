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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import com.harish.student_management.entity.Role;
import com.harish.student_management.entity.School;
import com.harish.student_management.entity.Teacher;
import com.harish.student_management.entity.User;

import com.harish.student_management.repository.TeacherRepository;
import com.harish.student_management.repository.UserRepository;
import com.harish.student_management.repository.RoleRepository;
import com.harish.student_management.repository.SchoolRepository;

import java.util.List;
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
    private final SchoolRepository schoolRepository;


    @Override
    @Transactional
    public UserResponseDto register(UserRequestDto request) {

        if (request.getUsername() == null
                || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        if (request.getEmail() == null
                || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (request.getPassword() == null
                || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (request.getSchoolCode() == null
                || request.getSchoolCode().isBlank()) {
            throw new IllegalArgumentException("School Code is required");
        }

        if (request.getRole() == null
                || request.getRole().isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();
        String schoolCode = request.getSchoolCode().trim();
        String normalizedRole = request.getRole().trim().toUpperCase();

        School school = schoolRepository.findByCode(schoolCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invalid School Code"
                        ));

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        // Privileged accounts must use the authorized admin workflow.
        if ("SUPER_ADMIN".equals(normalizedRole)
                || "SCHOOL_ADMIN".equals(normalizedRole)) {
            throw new IllegalArgumentException(
                    "Privileged accounts must be created through an authorized administration workflow"
            );
        }

        Role role = roleRepository.findByName(normalizedRole)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role not found: " + normalizedRole
                        ));

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .school(school)
                .status("ACTIVE")
                .build();

        User savedUser = userRepository.save(user);

        logger.info(
                "User registered successfully with username: {} and role: {}",
                savedUser.getUsername(),
                normalizedRole
        );

        return UserResponseDto.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(normalizedRole)
                .roles(savedUser.getAllRoleNames().stream().sorted().toList())
                .build();
    }


    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {

        String username = request.getUsername().trim();
        String schoolCode = request.getSchoolCode().trim();

        // Verify that the supplied school code exists.
        School requestedSchool = schoolRepository.findByCode(schoolCode)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid username, password, or School Code"
                        ));

        // Authenticate username and password.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsernameWithRole(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Ensure that the user belongs to the supplied school.
        if (user.getSchool() == null
                || !user.getSchool().getId().equals(requestedSchool.getId())) {
            throw new IllegalArgumentException(
                    "Invalid username, password, or School Code"
            );
        }

        // Load primary and additional roles.
        var roleNames = user.getAllRoleNames();

        // Preserve the existing teacher-profile validation.
        boolean hasTeacherRole = roleNames.stream()
                .anyMatch(role -> "TEACHER".equalsIgnoreCase(role));

        if (hasTeacherRole) {
            Teacher teacher = teacherRepository.findByUserId(user.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Teacher profile not found"));

            if (!"ACTIVE".equalsIgnoreCase(teacher.getStatus())) {
                throw new IllegalArgumentException(
                        "Your teacher account is inactive. Please contact the school administrator."
                );
            }
        }

        String token = jwtService.generateToken(user.getUsername());

        String primaryRole = user.getRole() == null
                ? null
                : user.getRole().getName();

        var allRoles = roleNames.stream()
                .sorted()
                .toList();

        Long schoolId = user.getSchool().getId();

        return LoginResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(primaryRole)
                .roles(allRoles)
                .schoolId(schoolId)
                .build();
    }


    @Override
    @Transactional
    public String forgotPassword(ForgotPasswordRequestDto request) {

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (request.getSchoolCode() == null
                || request.getSchoolCode().isBlank()) {
            throw new IllegalArgumentException("School Code is required");
        }

        String email = request.getEmail().trim();
        String schoolCode = request.getSchoolCode().trim();

        School school = schoolRepository.findByCode(schoolCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Invalid email or School Code"));

        User user = userRepository
                .findByEmailAndSchool_Id(email, school.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Invalid email or School Code"));

        // Generate a single-use reset token.
        String resetToken = UUID.randomUUID().toString();

        // Store only the hash in the database.
        user.setPasswordResetTokenHash(hashToken(resetToken));

        // Token expires after 30 minutes.
        user.setPasswordResetExpiresAt(
                LocalDateTime.now().plusMinutes(30)
        );

        userRepository.save(user);

        logger.info(
                "Password reset token generated for user: {}",
                user.getUsername()
        );

        // Retained for development/testing; use a secure email delivery
        // workflow instead of returning the token in production.
        return "Password reset token generated: " + resetToken;
    }


    @Override
    @Transactional
    public String resetPassword(ResetPasswordRequestDto request) {

        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new IllegalArgumentException("Reset token is required");
        }

        if (request.getSchoolCode() == null
                || request.getSchoolCode().isBlank()) {
            throw new IllegalArgumentException("School Code is required");
        }

        if (request.getNewPassword() == null
                || request.getConfirmPassword() == null
                || !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters"
            );
        }

        String schoolCode = request.getSchoolCode().trim();

        School requestedSchool = schoolRepository.findByCode(schoolCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invalid reset token or School Code"
                        ));

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
                                "Invalid or expired reset token"
                        ));

        // Ensure the reset token belongs to a user in the supplied school.
        if (user.getSchool() == null
                || !user.getSchool().getId().equals(requestedSchool.getId())) {
            throw new ResourceNotFoundException(
                    "Invalid reset token or School Code"
            );
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        // Invalidate the token after successful password reset.
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


    @Override
    @Transactional
    public UserResponseDto assignRole(Long userId, String roleName) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + userId));

        String normalizedRoleName = roleName.trim().toUpperCase();

        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role not found: " + normalizedRoleName));

        boolean alreadyHasRole = user.getAllRoleNames().stream()
                .anyMatch(existing ->
                        existing.equalsIgnoreCase(normalizedRoleName));

        if (!alreadyHasRole) {
            if (user.getRole() == null) {
                user.setRole(role);
            } else {
                user.addAdditionalRole(role);
            }

            user = userRepository.save(user);
        }

        List<String> roles = user.getAllRoleNames().stream()
                .sorted()
                .toList();

        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole() == null
                        ? null
                        : user.getRole().getName())
                .roles(roles)
                .build();
    }

}
