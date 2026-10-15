package com.harish.student_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkRequestDto {

    @NotNull(message = "Class ID is required")
    private Long classId;

    private Long sectionId;

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    private Long subjectId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private String status;

    private String attachmentUrl;

    private String priority;
}