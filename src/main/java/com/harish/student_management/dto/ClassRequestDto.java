package com.harish.student_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassRequestDto {

    @NotNull(message = "School ID is required")
    private Long schoolId;

    @NotBlank(message = "Class name is required")
    @Size(min = 1, max = 50, message = "Class name must be between 1 and 50 characters")
    private String name;

    @NotBlank(message = "Class code is required")
    @Size(min = 2, max = 20, message = "Class code must be between 2 and 20 characters")
    private String code;

    @Size(max = 10, message = "Grade must not exceed 10 characters")
    private String grade;

    private Integer year;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
