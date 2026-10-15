package com.harish.student_management.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassSubjectResponse {

    private Long id;

    private Long classId;

    private String className;

    private Long subjectId;

    private String subjectName;

    private String subjectCode;
}