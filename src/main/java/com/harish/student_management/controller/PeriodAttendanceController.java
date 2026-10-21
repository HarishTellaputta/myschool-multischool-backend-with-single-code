
package com.harish.student_management.controller;

import com.harish.student_management.dto.PeriodAttendanceRequestDto;
import com.harish.student_management.dto.PeriodAttendanceResponseDto;
import com.harish.student_management.service.PeriodAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance-periods")
@RequiredArgsConstructor
public class PeriodAttendanceController {

    private final PeriodAttendanceService periodAttendanceService;

    /**
     * Mark attendance for multiple students in a particular period.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<List<PeriodAttendanceResponseDto>> markAttendance(
            @Valid @RequestBody PeriodAttendanceRequestDto request) {

        return ResponseEntity.ok(
                periodAttendanceService.markPeriodAttendance(request)
        );
    }

    /**
     * Get attendance for a class, date and period.
     */
    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<List<PeriodAttendanceResponseDto>> getClassAttendance(
            @PathVariable Long classId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam Integer periodNumber) {

        return ResponseEntity.ok(
                periodAttendanceService.getClassPeriodAttendance(
                        classId, date, periodNumber)
        );
    }

    /**
     * Get a student's period-wise attendance history.
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    public ResponseEntity<List<PeriodAttendanceResponseDto>> getStudentAttendance(
            @PathVariable Long studentId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        return ResponseEntity.ok(
                periodAttendanceService.getStudentPeriodAttendance(
                        studentId, startDate, endDate)
        );
    }

    /**
     * Update a period attendance record.
     */
    @PutMapping("/{attendancePeriodId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<PeriodAttendanceResponseDto> updateAttendance(
            @PathVariable Long attendancePeriodId,
            @RequestBody PeriodAttendanceUpdateRequest request) {

        return ResponseEntity.ok(
                periodAttendanceService.updatePeriodAttendance(
                        attendancePeriodId,
                        request.getStatus(),
                        request.getRemarks())
        );
    }

    /**
     * Delete a period attendance record.
     */
    @DeleteMapping("/{attendancePeriodId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<Void> deleteAttendance(
            @PathVariable Long attendancePeriodId) {

        periodAttendanceService.deletePeriodAttendance(attendancePeriodId);
        return ResponseEntity.noContent().build();
    }

    public static class PeriodAttendanceUpdateRequest {

        @jakarta.validation.constraints.NotBlank(
                message = "Attendance status is required")
        @jakarta.validation.constraints.Pattern(
                regexp = "(?i)PRESENT|ABSENT|LEAVE",
                message = "Status must be PRESENT, ABSENT, or LEAVE")
        private String status;

        @jakarta.validation.constraints.Size(
                max = 500,
                message = "Remarks cannot exceed 500 characters")
        private String remarks;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }
    }
}
