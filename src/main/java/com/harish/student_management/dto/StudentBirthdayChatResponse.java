package com.harish.student_management.dto;


import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentBirthdayChatResponse {

    private Long studentId;

    private String studentName;

    private String admissionNo;

    private LocalDate dateOfBirth;

    private LocalDate birthdayDate;

    private boolean birthdayToday;

    private boolean photoAlreadyAdded;

    private Long birthdayStatusId;

    private String photoUrl;

    private String message;
}