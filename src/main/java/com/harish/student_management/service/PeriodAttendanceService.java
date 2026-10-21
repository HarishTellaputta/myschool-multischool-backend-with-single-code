
package com.harish.student_management.service;

import com.harish.student_management.dto.PeriodAttendanceRequestDto;
import com.harish.student_management.dto.PeriodAttendanceResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface PeriodAttendanceService {

    List<PeriodAttendanceResponseDto> markPeriodAttendance(
            PeriodAttendanceRequestDto request
    );

    List<PeriodAttendanceResponseDto> getClassPeriodAttendance(
            Long classId,
            LocalDate attendanceDate,
            Integer periodNumber
    );

    List<PeriodAttendanceResponseDto> getStudentPeriodAttendance(
            Long studentId,
            LocalDate startDate,
            LocalDate endDate
    );

    PeriodAttendanceResponseDto updatePeriodAttendance(
            Long attendancePeriodId,
            String status,
            String remarks
    );

    void deletePeriodAttendance(Long attendancePeriodId);
}
