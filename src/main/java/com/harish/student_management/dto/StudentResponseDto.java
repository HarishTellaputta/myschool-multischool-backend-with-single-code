package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponseDto {

    /** Backwards-compatible constructor retained for the original service tests. */
    public StudentResponseDto(
            Long id,
            String name,
            String email,
            String legacyCourse,
            String phone
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    private Long id;
    private String admissionNo;
    private String rollNumber;
    private String name;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodGroup;
    private LocalDate admissionDate;

    private Long classId;
    private String className;

    private Long sectionId;
    private String sectionName;

    private Long parentId;
    private String parentName;

    private Long academicYearId;
    private String academicYearName;

    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Boolean transportRequired;
}