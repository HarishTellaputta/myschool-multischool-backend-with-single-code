package com.harish.student_management.controller;

import com.harish.student_management.dto.*;
import com.harish.student_management.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendances")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "Mark single student attendance")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PostMapping
    public ResponseEntity<AttendanceResponseDto> markAttendance(@Valid @RequestBody AttendanceRequestDto request) {
        AttendanceResponseDto response = attendanceService.markAttendance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Mark bulk attendance for a class")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PostMapping("/bulk")
    public ResponseEntity<List<AttendanceResponseDto>> markBulkAttendance(@Valid @RequestBody BulkAttendanceRequestDto request) {
        List<AttendanceResponseDto> response = attendanceService.markBulkAttendance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get attendance by ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<AttendanceResponseDto> getAttendanceById(@PathVariable Long id) {
        AttendanceResponseDto response = attendanceService.getAttendanceById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update attendance")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<AttendanceResponseDto> updateAttendance(
            @PathVariable Long id, @Valid @RequestBody AttendanceUpdateDto request) {
        AttendanceResponseDto response = attendanceService.updateAttendance(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete attendance")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendance(@PathVariable Long id) {
        attendanceService.deleteAttendance(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get student attendance by date range")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<AttendanceResponseDto>> getStudentAttendance(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceResponseDto> response = attendanceService.getStudentAttendance(studentId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get class attendance for a specific date")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/class/{classId}")
    public ResponseEntity<List<AttendanceResponseDto>> getClassAttendance(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceResponseDto> response = attendanceService.getClassAttendance(classId, date);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get class attendance by date range")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/class/{classId}/range")
    public ResponseEntity<List<AttendanceResponseDto>> getClassAttendanceByDateRange(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceResponseDto> response = attendanceService.getClassAttendanceByDateRange(classId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get student attendance report")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @GetMapping("/student/{studentId}/report")
    public ResponseEntity<AttendanceReportDto> getStudentAttendanceReport(
            @PathVariable Long studentId,
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AttendanceReportDto response = attendanceService.getStudentAttendanceReport(studentId, classId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get class attendance report")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/class/{classId}/report")
    public ResponseEntity<List<AttendanceReportDto>> getClassAttendanceReport(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceReportDto> response = attendanceService.getClassAttendanceReport(classId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get parent's children attendance")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'PARENT')")
    @GetMapping("/parent/{parentId}")
    public ResponseEntity<List<AttendanceResponseDto>> getParentAttendance(
            @PathVariable Long parentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceResponseDto> response = attendanceService.getParentAttendance(parentId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Calculate attendance percentage")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @GetMapping("/student/{studentId}/percentage")
    public ResponseEntity<Double> calculateAttendancePercentage(
            @PathVariable Long studentId,
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Double response = attendanceService.calculateAttendancePercentage(studentId, classId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get today's attendance dashboard summary")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @GetMapping("/dashboard/today")
    public ResponseEntity<AttendanceDashboardSummaryDto> getDashboardSummary() {

        AttendanceDashboardSummaryDto response =
                attendanceService.getDashboardSummary();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get last 6 days attendance dashboard")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @GetMapping("/dashboard/last-six-days")
    public ResponseEntity<List<AttendanceLastSixDaysDto>> getLastSixDaysAttendance() {

        List<AttendanceLastSixDaysDto> response =
                attendanceService.getLastSixDaysAttendance();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update bulk attendance")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PutMapping("/bulk")
    public ResponseEntity<List<AttendanceResponseDto>> updateBulkAttendance(
            @Valid @RequestBody BulkAttendanceUpdateRequestDto request) {

        List<AttendanceResponseDto> response =
                attendanceService.updateBulkAttendance(request);

        return ResponseEntity.ok(response);
    }
}
