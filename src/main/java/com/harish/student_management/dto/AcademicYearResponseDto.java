package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicYearResponseDto {

    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean current;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
