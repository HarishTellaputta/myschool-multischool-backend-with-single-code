package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McqQuestionResponseDto {

    private Long id;

    private Long subjectId;
    private String subjectName;
    private String subjectCode;

    private Long classId;
    private String className;

    private Long createdByTeacherId;

    private LocalDate questionDate;

    private String question;
    private String questionImageUrl;

    private String optionA;
    private String optionAImageUrl;

    private String optionB;
    private String optionBImageUrl;

    private String optionC;
    private String optionCImageUrl;

    private String optionD;
    private String optionDImageUrl;

    private String correctAnswer;
    private Integer marks;
    private String explanation;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}