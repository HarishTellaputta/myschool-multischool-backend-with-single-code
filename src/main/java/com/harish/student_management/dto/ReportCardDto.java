package com.harish.student_management.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReportCardDto {
    private Long studentId; private String studentName; private String rollNumber;
    private Long classId; private String className; private Long sectionId; private String sectionName;
    private Long examinationId; private String examinationName;
    private List<ReportCardSubjectDto> subjects;
    private Integer totalMarks; private Integer totalMaximumMarks; private Double percentage; private String grade;
    private Integer rank; private Long attendancePresent; private Long attendanceTotal; private Double attendancePercentage;
    private String overallResult;
}
