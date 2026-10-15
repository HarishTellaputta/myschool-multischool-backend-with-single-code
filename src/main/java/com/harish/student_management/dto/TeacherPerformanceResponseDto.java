package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherPerformanceResponseDto {

    private Long classId;
    private String className;

    private Long sectionId;
    private String sectionName;

    private Long subjectId;
    private String subjectName;
    private String subjectCode;

    private Long latestExamScheduleId;
    private String latestExamName;
    private LocalDate examDate;

    private int studentCount;
    private int assessedCount;

    private double averageMarks;
    private int maxMarks;
    private double performancePercentage;
}