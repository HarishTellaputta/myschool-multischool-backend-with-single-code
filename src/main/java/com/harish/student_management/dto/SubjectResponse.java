package com.harish.student_management.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubjectResponse {

    private Long id;

    private Long schoolId;

    private String name;

    private String code;

    private String description;

    private String status;
}