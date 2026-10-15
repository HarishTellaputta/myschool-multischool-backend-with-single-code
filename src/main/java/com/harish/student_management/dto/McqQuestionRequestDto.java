package com.harish.student_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McqQuestionRequestDto {

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Class ID is required")
    private Long classId;

    private LocalDate questionDate;

    @NotBlank(message = "Question is required")
    private String question;

    @NotBlank(message = "Option A is required")
    private String optionA;

    @NotBlank(message = "Option B is required")
    private String optionB;

    @NotBlank(message = "Option C is required")
    private String optionC;

    @NotBlank(message = "Option D is required")
    private String optionD;

    @NotBlank(message = "Correct answer is required")
    @Pattern(
            regexp = "(?i)[ABCD]",
            message = "Correct answer must be A, B, C, or D"
    )
    private String correctAnswer;

    @NotNull(message = "Marks are required")
    @Positive(message = "Marks must be greater than zero")
    private Integer marks;

    private String explanation;

    private String questionImageUrl;

    private String optionAImageUrl;

    private String optionBImageUrl;

    private String optionCImageUrl;

    private String optionDImageUrl;
}