package com.harish.student_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceUpdateDto {

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    @NotBlank(message = "Status is required")
    private String status;

    private String remarks;
}
