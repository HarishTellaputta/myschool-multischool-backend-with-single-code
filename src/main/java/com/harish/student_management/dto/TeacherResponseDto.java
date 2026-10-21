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
public class TeacherResponseDto {

    private Long id;

    private Long schoolId;
    private String schoolName;

    private Long userId;

    private String employeeId;
    private String name;
    private String surname;
    private String email;
    private String phone;

    private LocalDate dateOfBirth;
    private String gender;
    private LocalDate joiningDate;

    private String qualification;
    private String designation;
    private String address;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}