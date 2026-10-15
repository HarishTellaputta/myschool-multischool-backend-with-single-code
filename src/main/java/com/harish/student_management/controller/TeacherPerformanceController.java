
        package com.harish.student_management.controller;

import com.harish.student_management.dto.SubjectPerformanceResponseDto;
import com.harish.student_management.dto.TeacherPerformanceResponseDto;
import com.harish.student_management.service.TeacherPerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TeacherPerformanceController {

    private final TeacherPerformanceService performanceService;

    // =========================================================
    // TEACHER LATEST PERFORMANCE
    // =========================================================

    @Operation(
            summary = "Get teacher class and subject latest exam performance"
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @GetMapping("/teachers/{teacherId}/performance")
    public ResponseEntity<List<TeacherPerformanceResponseDto>>
    getTeacherPerformance(
            @PathVariable Long teacherId) {

        return ResponseEntity.ok(
                performanceService.getTeacherPerformance(
                        teacherId
                )
        );
    }

    // =========================================================
    // SUBJECT PERFORMANCE HISTORY
    // =========================================================

    @Operation(
            summary = "Get subject class-wise examination performance history"
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @GetMapping("/subjects/{subjectId}/performance")
    public ResponseEntity<List<SubjectPerformanceResponseDto>>
    getSubjectPerformance(
            @PathVariable Long subjectId) {

        return ResponseEntity.ok(
                performanceService.getSubjectPerformance(
                        subjectId
                )
        );
    }


    @GetMapping("/teachers/me/performance")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<TeacherPerformanceResponseDto>>
    getMyPerformance(Authentication authentication) {

        return ResponseEntity.ok(
                performanceService.getMyPerformance(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/teachers/me/performance/subjects")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<SubjectPerformanceResponseDto>>
    getMySubjectPerformance(Authentication authentication) {

        return ResponseEntity.ok(
                performanceService.getMySubjectPerformance(
                        authentication.getName()
                )
        );
    }
}

