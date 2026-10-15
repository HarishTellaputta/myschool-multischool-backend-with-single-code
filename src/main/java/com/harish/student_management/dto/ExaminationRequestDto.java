package com.harish.student_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExaminationRequestDto {

    private Long academicYearId;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "Exam type is required")
    private String examType;

    private Integer year;

    private String status;
}
