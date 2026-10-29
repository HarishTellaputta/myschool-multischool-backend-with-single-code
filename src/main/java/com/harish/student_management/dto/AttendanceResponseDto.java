
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
public class AttendanceResponseDto {

    private Long id;

    private Long studentId;
    private String studentName;
    private String studentRollNumber;

    private Long classId;
    private String className;

    private Long markedByTeacherId;
    private String markedByTeacherName;

    private LocalDate attendanceDate;

    /**
     * 0 = SINGLE mode (daily attendance)
     * 1..N = PERIOD_WISE mode
     */
    private Integer periodNumber;

    private String status;
    private String remarks;

    private LocalDateTime markedAt;
    private LocalDateTime updatedAt;

    private Long updatedByTeacherId;
    private String updatedByTeacherName;
}
