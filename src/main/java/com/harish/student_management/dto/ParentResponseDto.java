package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentResponseDto {

    private Long id;
    private String fatherName;
    private String surname;
    private String motherName;
    private String guardianName;
    private String contactPhone;
    private String contactEmail;
    private String relationship;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<StudentResponseDto> students;
}