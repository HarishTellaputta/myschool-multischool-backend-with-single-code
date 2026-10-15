package com.harish.student_management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamScheduleRequestDto {

    @NotNull(message = "Examination ID is required")
    private Long examinationId;

    @NotNull(message = "Class ID is required")
    private Long classId;

    private Long sectionId;

    private Long subjectTeacherId;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Exam date is required")
    private LocalDate examDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be positive")
    private Integer duration;

    @NotNull(message = "Maximum marks is required")
    @Positive(message = "Maximum marks must be positive")
    private Integer maxMarks;

    @NotBlank(message = "Exam type is required")
    private String examType;

    private String roomNumber;

    private String status;
}
