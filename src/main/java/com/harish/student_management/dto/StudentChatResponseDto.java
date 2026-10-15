package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentChatResponseDto {

    private Long studentId;
    private String studentName;

    private Long parentId;
    private String parentName;

    private Long classId;
    private String className;

    private Long sectionId;
    private String sectionName;
}