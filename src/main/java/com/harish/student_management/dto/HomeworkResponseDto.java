package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkResponseDto {

    private Long id;

    private Long classId;
    private String className;

    private Long sectionId;
    private String sectionName;

    private Long assignedByTeacherId;
    private String assignedByTeacherName;
    private String teacherEmployeeId;

    private Long subjectId;
    private String subjectName;
    private String subjectCode;

    private String title;
    private String description;

    private LocalDate dueDate;

    private String status;

    private String attachmentUrl;

    private String priority;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}