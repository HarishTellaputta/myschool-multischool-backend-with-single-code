package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceLastSixDaysDto {

    private LocalDate date;
    private String day;
    private double attendancePercentage;
    private long present;
    private long absent;
    private long leave;
    private long totalStudents;
    private long marked;
}