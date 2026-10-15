package com.harish.student_management.controller;

import com.harish.student_management.dto.TeacherLeaveApprovalDto;
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
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<TeacherLeaveResponseDto> applyForLeave(@Valid @RequestBody TeacherLeaveRequestDto request) {
        TeacherLeaveResponseDto response = leaveService.applyForLeave(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get leave by ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<TeacherLeaveResponseDto> getLeaveById(@PathVariable Long id) {
        TeacherLeaveResponseDto response = leaveService.getLeaveById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get leaves by teacher ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<TeacherLeaveResponseDto>> getLeavesByTeacherId(@PathVariable Long teacherId) {
        List<TeacherLeaveResponseDto> response = leaveService.getLeavesByTeacherId(teacherId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get leaves by teacher ID and status")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/teacher/{teacherId}/status/{status}")
    public ResponseEntity<List<TeacherLeaveResponseDto>> getLeavesByTeacherIdAndStatus(
            @PathVariable Long teacherId, @PathVariable String status) {
        List<TeacherLeaveResponseDto> response = leaveService.getLeavesByTeacherIdAndStatus(teacherId, status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get leaves by status (for admin)")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TeacherLeaveResponseDto>> getLeavesByStatus(@PathVariable String status) {
        List<TeacherLeaveResponseDto> response = leaveService.getLeavesByStatus(status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Approve or reject leave")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PutMapping("/{id}/approve")
    public ResponseEntity<TeacherLeaveResponseDto> approveLeave(
            @PathVariable Long id,
            @Valid @RequestBody TeacherLeaveApprovalDto approval,
            Authentication authentication) {
        String approvedBy = authentication.getName();
        TeacherLeaveResponseDto response = leaveService.approveLeave(id, approval, approvedBy);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update leave application")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<TeacherLeaveResponseDto> updateLeave(
            @PathVariable Long id, @Valid @RequestBody TeacherLeaveRequestDto request) {
        TeacherLeaveResponseDto response = leaveService.updateLeave(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete leave application")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeave(@PathVariable Long id) {
        leaveService.deleteLeave(id);
        return ResponseEntity.noContent().build();
    }
}
