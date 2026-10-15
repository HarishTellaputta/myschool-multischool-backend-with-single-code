package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamScheduleResponseDto {

    private Long id;

    private Long examinationId;
    private String examinationName;

    private Long classId;
    private String className;

    private Long sectionId;
    private String sectionName;

    private Long subjectId;
    private String subjectName;
    private String subjectCode;

    private Long subjectTeacherId;
    private String subjectTeacherName;

    private LocalDate examDate;
    private LocalTime startTime;
    private Integer duration;
    private Integer maxMarks;

    private String examType;
    private String roomNumber;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}