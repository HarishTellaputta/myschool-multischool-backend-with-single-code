
package com.harish.student_management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAttendanceUpdateRequestDto {

    private Long teacherId;

    @NotEmpty(message = "Attendance records cannot be empty")
    @Valid
    private List<AttendanceUpdateRecordDto> attendanceRecords;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceUpdateRecordDto {

        @NotNull(message = "Attendance ID is required")
        private Long attendanceId;

        @NotNull(message = "Status is required")
        private String status;

        private String remarks;
    }
}
