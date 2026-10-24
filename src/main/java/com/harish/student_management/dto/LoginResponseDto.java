package com.harish.student_management.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {

    private String token;
    private Long userId;
    private String username;

    // Primary role
    private String role;

    // All assigned roles
    @Builder.Default
    private List<String> roles = List.of();

    // School associated with this user; null when not applicable
    private Long schoolId;
}