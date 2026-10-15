package com.harish.student_management.dto;


import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDashboardSummaryDto {

    private long totalStudents;

    private long marked;

    private long present;

    private long absent;

    private long leave;

    private double attendancePercentage;
}