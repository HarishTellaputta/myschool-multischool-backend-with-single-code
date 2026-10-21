
package com.harish.student_management.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private Long id;
    private String username;
    private String email;

    // Primary role retained for existing clients.
    private String role;

    // All roles assigned to the user.
    @Builder.Default
    private List<String> roles = List.of();
}
