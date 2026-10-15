package com.harish.student_management.controller;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.*;
import com.harish.student_management.repository.*;
import com.harish.student_management.service.AttendanceService;
import com.harish.student_management.service.ExaminationService;
import com.harish.student_management.service.FeeService;
import com.harish.student_management.service.McqTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final AttendanceService attendanceService;
    private final ExaminationService examinationService;
    private final McqTestService mcqTestService;
    private final FeeService feeService;

    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public Page<AttendanceReportDto> attendance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable page) {

        validateRange(from, to);
        return attendanceService.getAttendanceReport(from, to, page);
    }


    @GetMapping("/fees")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public Page<FeePaymentResponseDto> fees(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            Pageable page) {

        return feeService.getPaymentReport(
                from,
                to,
                page
        );
    }

    @GetMapping("/examinations")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public Page<ExamScheduleResponseDto> examinations(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            Pageable page) {

        return examinationService.getExamScheduleReport(
                from,
                to,
                page
        );
    }

    @GetMapping("/mcq-performance")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public Page<McqAttemptResponseDto> mcq(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false)
            Long classId,

            @RequestParam(required = false)
            Long sectionId,

            @RequestParam(required = false)
            String subject,

            Pageable page
    ) {

        return mcqTestService.reportPerformance(
                date,
                classId,
                sectionId,
                subject,
                page
        );
    }


    private void validateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw new IllegalArgumentException("End date must not precede start date");
    }
}
