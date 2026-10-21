package com.harish.student_management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSchoolWithAdminRequestDto {

    @Valid
    private SchoolRequestDto school;

    @NotBlank(message = "Admin username is required")
    @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
    private String adminUsername;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Enter a valid admin email")
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String adminPassword;
}