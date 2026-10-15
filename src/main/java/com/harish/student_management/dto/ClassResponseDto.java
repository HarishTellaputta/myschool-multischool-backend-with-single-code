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
public class ClassResponseDto {

    private Long id;
    private Long schoolId;
    private String schoolName;
    private String name;
    private String code;
    private String grade;
    private Integer year;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
