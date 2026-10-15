package com.harish.student_management.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {

    private String token;

    private Long userId;

    private String username;

    private String role;
}