package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceReportDto {

    private Long studentId;
    private String studentName;
    private String studentRollNumber;
    private Long classId;
    private String className;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalDays;
    private int presentDays;
    private int absentDays;
    private int leaveDays;
    private double attendancePercentage;
}
