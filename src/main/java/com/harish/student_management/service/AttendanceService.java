package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    AttendanceResponseDto markAttendance(AttendanceRequestDto request);

    List<AttendanceResponseDto> markBulkAttendance(BulkAttendanceRequestDto request);

    AttendanceResponseDto getAttendanceById(Long id);

    AttendanceResponseDto updateAttendance(Long id, AttendanceUpdateDto request);

    void deleteAttendance(Long id);

    List<AttendanceResponseDto> getStudentAttendance(Long studentId, LocalDate startDate, LocalDate endDate);

    List<AttendanceResponseDto> getClassAttendance(Long classId, LocalDate date);

    List<AttendanceResponseDto> getClassAttendanceByDateRange(Long classId, LocalDate startDate, LocalDate endDate);

    AttendanceReportDto getStudentAttendanceReport(Long studentId, Long classId, LocalDate startDate, LocalDate endDate);

    List<AttendanceReportDto> getClassAttendanceReport(Long classId, LocalDate startDate, LocalDate endDate);

    List<AttendanceResponseDto> getParentAttendance(Long parentId, LocalDate startDate, LocalDate endDate);

    double calculateAttendancePercentage(Long studentId, Long classId, LocalDate startDate, LocalDate endDate);
    AttendanceDashboardSummaryDto getDashboardSummary();

    List<AttendanceLastSixDaysDto> getLastSixDaysAttendance();

    Page<AttendanceReportDto> getAttendanceReport(
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );
    List<AttendanceResponseDto> updateBulkAttendance(
            BulkAttendanceUpdateRequestDto request
    );
}
