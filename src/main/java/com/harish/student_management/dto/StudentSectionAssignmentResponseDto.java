package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSectionAssignmentResponseDto {

    private Long id;
    private Long studentId;
    private String studentName;
    private String admissionNo;
    private Long sectionId;
    private String sectionName;
    private Long classId;
    private String className;
    private Long academicYearId;
    private String academicYearName;
    private LocalDateTime assignedAt;
}
