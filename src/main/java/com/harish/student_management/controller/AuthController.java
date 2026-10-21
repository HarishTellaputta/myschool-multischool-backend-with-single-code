package com.harish.student_management.controller;

import com.harish.student_management.dto.*;
import com.harish.student_management.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register New User")
    @PostMapping("/register")
    public UserResponseDto register(
            @Valid @RequestBody UserRequestDto request) {

        return authService.register(request);
    }

    @Operation(summary = "Login User")
    @PostMapping("/login")
    public LoginResponseDto login(
            @Valid @RequestBody LoginRequestDto request) {

        return authService.login(request);
    }

    @Operation(summary = "Forgot Password")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDto request) {

        String response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reset Password")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request) {

        String response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change Password")
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto request,
            Authentication authentication) {

        String username = authentication.getName();
        String response = authService.changePassword(request, username);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Logout User")
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponseDto> logout(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        LogoutResponseDto response = authService.logout(token);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Assign an additional role to a user")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<UserResponseDto> assignRole(
            @PathVariable Long userId,
            @Valid @RequestBody RoleAssignmentRequestDto request) {

        UserResponseDto response =
                authService.assignRole(userId, request.getRole());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}