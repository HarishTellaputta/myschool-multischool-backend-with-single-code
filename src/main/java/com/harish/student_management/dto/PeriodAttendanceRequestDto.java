
package com.harish.student_management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodAttendanceRequestDto {

    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    @NotNull(message = "Period number is required")
    @Min(value = 1, message = "Period number must be at least 1")
    @Max(value = 20, message = "Period number cannot exceed 20")
    private Integer periodNumber;

    @NotNull(message = "Attendance records are required")
    @Size(min = 1, message = "At least one student attendance record is required")
    private List<StudentPeriodAttendanceDto> students;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentPeriodAttendanceDto {

        @NotNull(message = "Student ID is required")
        private Long studentId;

        @NotNull(message = "Attendance status is required")
        @Pattern(
                regexp = "(?i)PRESENT|ABSENT|LEAVE",
                message = "Status must be PRESENT, ABSENT, or LEAVE"
        )
        private String status;

        @Size(max = 500, message = "Remarks cannot exceed 500 characters")
        private String remarks;
    }
}
