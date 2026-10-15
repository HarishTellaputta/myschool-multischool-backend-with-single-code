package com.harish.student_management.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class GradeRuleResponseDto {
    private Long id;
    private String grade;
    private Double minimumPercentage;
    private Double maximumPercentage;
}
