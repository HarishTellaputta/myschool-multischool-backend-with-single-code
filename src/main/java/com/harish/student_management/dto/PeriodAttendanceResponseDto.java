
package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodAttendanceResponseDto {

    private Long id;

    private Long studentId;
    private String studentName;
    private String rollNumber;

    private Long classId;
    private String className;

    private LocalDate attendanceDate;
    private Integer periodNumber;

    private String status;
    private String remarks;

    private Long markedByTeacherId;
    private String markedByTeacherName;

    private Long updatedByTeacherId;
    private String updatedByTeacherName;

    private LocalDateTime markedAt;
    private LocalDateTime updatedAt;
}
