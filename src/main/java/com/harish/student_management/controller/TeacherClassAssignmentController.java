package com.harish.student_management.controller;

import com.harish.student_management.dto.TeacherClassAssignmentRequestDto;
import com.harish.student_management.dto.TeacherClassAssignmentResponseDto;
import com.harish.student_management.service.TeacherClassAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/teacher-class-assignments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TeacherClassAssignmentController {

    private final TeacherClassAssignmentService assignmentService;


    // =========================================================
    // ASSIGN TEACHER TO CLASS + SUBJECT
    // =========================================================

    @Operation(
            summary = "Assign teacher to class and subject"
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PostMapping
    public ResponseEntity<TeacherClassAssignmentResponseDto>
    assignTeacherToClass(
            @Valid @RequestBody TeacherClassAssignmentRequestDto request) {

        TeacherClassAssignmentResponseDto response =
                assignmentService.assignTeacherToClass(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =========================================================
    // GET ASSIGNMENT BY ID
    // =========================================================

    @Operation(
            summary = "Get assignment by ID"
    )
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<TeacherClassAssignmentResponseDto>
    getAssignmentById(
            @PathVariable Long id) {

        TeacherClassAssignmentResponseDto response =
                assignmentService.getAssignmentById(id);

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // GET ASSIGNMENTS BY TEACHER
    // =========================================================

    @Operation(
            summary = "Get assignments by teacher ID"
    )
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<TeacherClassAssignmentResponseDto>>
    getAssignmentsByTeacherId(
            @PathVariable Long teacherId) {

        List<TeacherClassAssignmentResponseDto> response =
                assignmentService.getAssignmentsByTeacherId(
                        teacherId
                );

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // GET ASSIGNMENTS BY CLASS
    // =========================================================

    @Operation(
            summary = "Get assignments by class ID"
    )
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/class/{classId}")
    public ResponseEntity<List<TeacherClassAssignmentResponseDto>>
    getAssignmentsByClassId(
            @PathVariable Long classId) {

        List<TeacherClassAssignmentResponseDto> response =
                assignmentService.getAssignmentsByClassId(
                        classId
                );

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // GET ASSIGNMENTS BY TEACHER + SUBJECT
    // =========================================================

    @Operation(
            summary = "Get assignments by teacher ID and subject ID"
    )
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/teacher/{teacherId}/subject/{subjectId}")
    public ResponseEntity<List<TeacherClassAssignmentResponseDto>>
    getAssignmentsByTeacherIdAndSubject(
            @PathVariable Long teacherId,
            @PathVariable Long subjectId) {

        List<TeacherClassAssignmentResponseDto> response =
                assignmentService.getAssignmentsByTeacherIdAndSubject(
                        teacherId,
                        subjectId
                );

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // GET SUBJECT NAMES BY TEACHER
    // =========================================================

    @Operation(
            summary = "Get subjects by teacher ID"
    )
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/teacher/{teacherId}/subjects")
    public ResponseEntity<Set<String>>
    getSubjectsByTeacherId(
            @PathVariable Long teacherId) {

        Set<String> response =
                assignmentService.getSubjectsByTeacherId(
                        teacherId
                );

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // REMOVE ASSIGNMENT
    // =========================================================

    @Operation(
            summary = "Remove teacher class subject assignment"
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeAssignment(
            @PathVariable Long id) {

        assignmentService.removeAssignment(id);

        return ResponseEntity.noContent().build();
    }
}