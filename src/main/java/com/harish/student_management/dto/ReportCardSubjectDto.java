package com.harish.student_management.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReportCardSubjectDto {
    private Long scheduleId; private String subject; private java.time.LocalDate examDate;
    private Integer marksObtained; private Integer maximumMarks; private Double percentage; private String grade;
}
