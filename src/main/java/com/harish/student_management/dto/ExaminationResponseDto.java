package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExaminationResponseDto {

    private Long id;
    private Long academicYearId;
    private String academicYearName;
    private String name;
    private String description;
    private String examType;
    private Integer year;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
