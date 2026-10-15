package com.harish.student_management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResultRequestDto {

    @NotNull(message = "Exam schedule ID is required")
    private Long examScheduleId;

    @NotNull(message = "Student ID is required")
    private Long studentId;

    private Long teacherId;

    @NotNull(message = "Marks obtained is required")
    @Min(value = 0, message = "Marks cannot be negative")
    private Integer marksObtained;

    private Integer maxMarks;

    private String grade;

    private String remarks;

    private String status;
}
