package com.harish.student_management.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class GradeRuleRequestDto {
    @NotBlank private String grade;
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") private Double minimumPercentage;
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") private Double maximumPercentage;
}
