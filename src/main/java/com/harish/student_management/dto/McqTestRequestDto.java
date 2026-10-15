package com.harish.student_management.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McqTestRequestDto {
    @NotNull
    private Long classId;
    private Long sectionId;
    @NotBlank
    private String subject;
    @NotNull
    private LocalDate date;
    @NotNull
    private LocalTime startTime;
    @NotNull
    @Positive
    private Integer duration;
    @NotNull
    @Positive
    private Integer numberOfQuestions;
}
