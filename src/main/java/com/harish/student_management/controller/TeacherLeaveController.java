
        package com.harish.student_management.controller;

import com.harish.student_management.dto.TeacherLeaveRequestDto;
import com.harish.student_management.dto.TeacherLeaveResponseDto;
import com.harish.student_management.service.TeacherLeaveService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-leaves")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TeacherLeaveController {

    private final TeacherLeaveService leaveService;

    @Operation(summary = "Apply for leave")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PostMapping
    public ResponseEntity<TeacherLeaveResponseDto> applyForLeave(
            @Valid @RequestBody TeacherLeaveRequestDto request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveService.applyForLeave(request));
    }

    @Operation(summary = "Get all leaves for current school (admin only)")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @GetMapping
    public ResponseEntity<List<TeacherLeaveResponseDto>> getAllSchoolLeaves() {

        return ResponseEntity.ok(leaveService.getAllSchoolLeaves());
    }

    @Operation(summary = "Get a leave application by ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<TeacherLeaveResponseDto> getLeaveById(
            @PathVariable Long id) {

        return ResponseEntity.ok(leaveService.getLeaveById(id));
    }

    @Operation(summary = "Get leaves by teacher ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<TeacherLeaveResponseDto>> getLeavesByTeacherId(
            @PathVariable Long teacherId) {

        return ResponseEntity.ok(
                leaveService.getLeavesByTeacherId(teacherId));
    }

    @Operation(summary = "Get leaves for a year")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/year/{year}")
    public ResponseEntity<List<TeacherLeaveResponseDto>> getLeavesByYear(
            @PathVariable int year) {

        return ResponseEntity.ok(
                leaveService.getLeavesByYear(year));
    }

    @Operation(summary = "Get leaves for a specific year and month")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/year/{year}/month/{month}")
    public ResponseEntity<List<TeacherLeaveResponseDto>>
    getLeavesByYearAndMonth(
            @PathVariable int year,
            @PathVariable int month) {

        return ResponseEntity.ok(
                leaveService.getLeavesByYearAndMonth(year, month));
    }

    @Operation(summary = "Update a leave application")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<TeacherLeaveResponseDto> updateLeave(
            @PathVariable Long id,
            @Valid @RequestBody TeacherLeaveRequestDto request) {

        return ResponseEntity.ok(
                leaveService.updateLeave(id, request));
    }

    @Operation(summary = "Delete a leave application")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeave(
            @PathVariable Long id) {

        leaveService.deleteLeave(id);
        return ResponseEntity.noContent().build();
    }
}
