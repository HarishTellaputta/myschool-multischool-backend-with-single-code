
package com.harish.student_management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSettingsDto {

    @NotBlank(message = "Attendance mode is required")
    @Pattern(
            regexp = "(?i)SINGLE|PERIOD_WISE",
            message = "Attendance mode must be SINGLE or PERIOD_WISE"
    )
    private String attendanceMode;

    @NotNull(message = "Attendance period count is required")
    @Min(value = 1, message = "At least one period is required")
    @Max(value = 20, message = "Attendance period count cannot exceed 20")
    private Integer attendancePeriodCount;
}
