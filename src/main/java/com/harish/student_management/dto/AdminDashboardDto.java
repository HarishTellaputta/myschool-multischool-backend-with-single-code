package com.harish.student_management.dto;

import lombok.*;

import java.math.*;
import java.util.*;

@Data
@Builder
public class AdminDashboardDto {
    Long totalStudents, totalTeachers, presentStudents, absentStudents, admissions, homework, events, notifications;
    BigDecimal pendingFees;
    List<ExamScheduleResponseDto> upcomingExams;
}
