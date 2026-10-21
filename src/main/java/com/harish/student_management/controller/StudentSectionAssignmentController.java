package com.harish.student_management.controller;

import com.harish.student_management.dto.StudentSectionAssignmentRequestDto;
import com.harish.student_management.dto.StudentSectionAssignmentResponseDto;
import com.harish.student_management.service.StudentSectionAssignmentService;
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
@RequestMapping("/api/v1/student-section-assignments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StudentSectionAssignmentController {

    private final StudentSectionAssignmentService assignmentService;

    @Operation(summary = "Assign student to section")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PostMapping
    public ResponseEntity<StudentSectionAssignmentResponseDto> assignStudentToSection(
            @Valid @RequestBody StudentSectionAssignmentRequestDto request) {
        StudentSectionAssignmentResponseDto response = assignmentService.assignStudentToSection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get assignment by ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<StudentSectionAssignmentResponseDto> getAssignmentById(@PathVariable Long id) {
        StudentSectionAssignmentResponseDto response = assignmentService.getAssignmentById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get assignments by student ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<StudentSectionAssignmentResponseDto>> getAssignmentsByStudentId(@PathVariable Long studentId) {
        List<StudentSectionAssignmentResponseDto> response = assignmentService.getAssignmentsByStudentId(studentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get assignments by section ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/section/{sectionId}")
    public ResponseEntity<List<StudentSectionAssignmentResponseDto>> getAssignmentsBySectionId(@PathVariable Long sectionId) {
        List<StudentSectionAssignmentResponseDto> response = assignmentService.getAssignmentsBySectionId(sectionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get assignments by academic year ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/academic-year/{academicYearId}")
    public ResponseEntity<List<StudentSectionAssignmentResponseDto>> getAssignmentsByAcademicYearId(@PathVariable Long academicYearId) {
        List<StudentSectionAssignmentResponseDto> response = assignmentService.getAssignmentsByAcademicYearId(academicYearId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get assignments by section and academic year")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/section/{sectionId}/academic-year/{academicYearId}")
    public ResponseEntity<List<StudentSectionAssignmentResponseDto>> getAssignmentsBySectionAndYear(
            @PathVariable Long sectionId,
            @PathVariable Long academicYearId) {
        List<StudentSectionAssignmentResponseDto> response = assignmentService.getAssignmentsBySectionAndYear(sectionId, academicYearId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove assignment")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeAssignment(@PathVariable Long id) {
        assignmentService.removeAssignment(id);
        return ResponseEntity.noContent().build();
    }
}
