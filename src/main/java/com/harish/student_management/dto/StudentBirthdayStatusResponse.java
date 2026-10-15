package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentBirthdayStatusResponse {

    private Long id;

    private Long studentId;

    private String studentName;

    private String admissionNo;

    private LocalDate dateOfBirth;

    private LocalDate birthdayDate;

    private String photoUrl;

    private LocalDateTime createdAt;
}