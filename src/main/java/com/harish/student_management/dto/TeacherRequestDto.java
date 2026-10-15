package com.harish.student_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherRequestDto {

    @NotNull(message = "School ID is required")
    private Long schoolId;

    @NotNull(message = "Registered teacher user ID is required")
    private Long userId;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private LocalDate dateOfBirth;

    private String gender;

    private LocalDate joiningDate;

    private String qualification;

    private String designation;

    private String address;

    private String status;
}