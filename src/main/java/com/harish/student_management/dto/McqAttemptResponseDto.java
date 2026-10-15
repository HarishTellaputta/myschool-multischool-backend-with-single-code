package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McqAttemptResponseDto {

    private Long attemptId;
    private Long testId;

    // Student details
    private Long studentId;
    private String studentName;
    private String admissionNo;
    private Long sectionId;
    private String sectionName;

    private String status;

    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime submittedAt;

    private Integer score;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer wrongAnswers;

    private Double percentage;

    private McqTestResponseDto test;
}

