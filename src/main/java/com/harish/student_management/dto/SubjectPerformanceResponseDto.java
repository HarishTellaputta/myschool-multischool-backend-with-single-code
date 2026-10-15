
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
public class SubjectPerformanceResponseDto {

    private Long classId;
    private String className;

    private Long sectionId;
    private String sectionName;

    private Long subjectId;
    private String subjectName;
    private String subjectCode;

    private Long examScheduleId;
    private String examName;
    private LocalDate examDate;

    private Integer studentCount;
    private Integer assessedCount;

    private Double averageMarks;
    private Integer maxMarks;

    private Double performancePercentage;

}

