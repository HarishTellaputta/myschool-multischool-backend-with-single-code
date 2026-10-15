package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructureResponseDto {

    private Long id;

    private String name;

    private Long classId;
    private String className;

    private Long academicYearId;
    private String academicYearName;

    private BigDecimal amount;

    private LocalDate dueDate;

    private String status;

    private boolean applied;
}