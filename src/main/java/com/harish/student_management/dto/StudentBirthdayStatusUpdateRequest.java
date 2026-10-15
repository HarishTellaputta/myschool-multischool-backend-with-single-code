package com.harish.student_management.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentBirthdayStatusUpdateRequest {

    @NotNull(message = "Birthday date is required")
    private LocalDate birthdayDate;

    @Size(max = 500, message = "Photo URL must not exceed 500 characters")
    private String photoUrl;
}
