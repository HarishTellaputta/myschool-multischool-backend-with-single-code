package com.harish.student_management.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.*;
import java.time.*;

@Data
public class FeeStructureRequestDto {
    @NotBlank
    private String name;
    @NotNull
    private Long classId;
    private Long academicYearId;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    @NotNull
    private LocalDate dueDate;
    private String status;
}
