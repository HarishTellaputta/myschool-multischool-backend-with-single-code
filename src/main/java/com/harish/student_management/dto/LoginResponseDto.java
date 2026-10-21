
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

    // Backward compatibility: primary role.
    private String role;

    // All roles assigned to this user.
    @Builder.Default
    private List<String> roles = List.of();
}
