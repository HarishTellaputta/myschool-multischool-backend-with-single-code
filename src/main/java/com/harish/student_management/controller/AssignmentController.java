package com.harish.student_management.controller;

import com.harish.student_management.dto.AssignmentRequestDto;
import com.harish.student_management.dto.AssignmentResponseDto;
import com.harish.student_management.service.AssignmentService;
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
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AssignmentController {

    private final AssignmentService assignmentService;

    // ============================================================
    // CREATE
    // ============================================================

    @Operation(summary = "Create assignment")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PostMapping
    public ResponseEntity<AssignmentResponseDto> createAssignment(
            @Valid @RequestBody AssignmentRequestDto request) {

        AssignmentResponseDto response =
                assignmentService.createAssignment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ============================================================
    // GET BY ID
    // ============================================================

    @Operation(summary = "Get assignment by ID")
    @PreAuthorize(
            "hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')"
    )
    @GetMapping("/{id}")
    public ResponseEntity<AssignmentResponseDto> getAssignmentById(
            @PathVariable Long id) {

        AssignmentResponseDto response =
                assignmentService.getAssignmentById(id);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET ALL
    // ============================================================

    @Operation(summary = "Get all assignments")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @GetMapping
    public ResponseEntity<List<AssignmentResponseDto>> getAllAssignments() {

        List<AssignmentResponseDto> response =
                assignmentService.getAllAssignments();

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET BY CLASS
    // ============================================================

    @Operation(summary = "Get assignments by class ID")
    @PreAuthorize(
            "hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')"
    )
    @GetMapping("/class/{classId}")
    public ResponseEntity<List<AssignmentResponseDto>>
    getAssignmentsByClassId(
            @PathVariable Long classId) {

        List<AssignmentResponseDto> response =
                assignmentService.getAssignmentsByClassId(classId);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET BY SECTION
    // ============================================================

    @Operation(summary = "Get assignments by section ID")
    @PreAuthorize(
            "hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')"
    )
    @GetMapping("/section/{sectionId}")
    public ResponseEntity<List<AssignmentResponseDto>>
    getAssignmentsBySectionId(
            @PathVariable Long sectionId) {

        List<AssignmentResponseDto> response =
                assignmentService.getAssignmentsBySectionId(sectionId);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET BY CLASS + SECTION
    // ============================================================

    @Operation(summary = "Get assignments by class ID and section ID")
    @PreAuthorize(
            "hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')"
    )
    @GetMapping("/class/{classId}/section/{sectionId}")
    public ResponseEntity<List<AssignmentResponseDto>>
    getAssignmentsByClassIdAndSectionId(
            @PathVariable Long classId,
            @PathVariable Long sectionId) {

        List<AssignmentResponseDto> response =
                assignmentService.getAssignmentsByClassIdAndSectionId(
                        classId,
                        sectionId
                );

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET BY TEACHER
    // ============================================================

    @Operation(summary = "Get assignments by teacher ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<AssignmentResponseDto>>
    getAssignmentsByTeacherId(
            @PathVariable Long teacherId) {

        List<AssignmentResponseDto> response =
                assignmentService.getAssignmentsByTeacherId(teacherId);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET BY SUBJECT ID
    // ============================================================

    @Operation(summary = "Get assignments by subject ID")
    @PreAuthorize(
            "hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')"
    )
    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<List<AssignmentResponseDto>>
    getAssignmentsBySubject(
            @PathVariable Long subjectId) {

        List<AssignmentResponseDto> response =
                assignmentService.getAssignmentsBySubject(subjectId);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET BY CLASS + DATE RANGE
    // ============================================================

    @Operation(summary = "Get assignments by class ID and due date range")
    @PreAuthorize(
            "hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')"
    )
    @GetMapping("/class/{classId}/date-range")
    public ResponseEntity<List<AssignmentResponseDto>>
    getAssignmentsByClassIdAndDueDateRange(
            @PathVariable Long classId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        List<AssignmentResponseDto> response =
                assignmentService.getAssignmentsByClassIdAndDueDateRange(
                        classId,
                        startDate,
                        endDate
                );

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET ACTIVE BY CLASS
    // ============================================================

    @Operation(summary = "Get active assignments by class ID")
    @PreAuthorize(
            "hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')"
    )
    @GetMapping("/class/{classId}/active")
    public ResponseEntity<List<AssignmentResponseDto>>
    getActiveAssignmentsByClassId(
            @PathVariable Long classId) {

        List<AssignmentResponseDto> response =
                assignmentService.getActiveAssignmentsByClassId(classId);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET BY CLASS + SUBJECT ID
    // ============================================================

    @Operation(summary = "Get assignments by class ID and subject ID")
    @PreAuthorize(
            "hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')"
    )
    @GetMapping("/class/{classId}/subject/{subjectId}")
    public ResponseEntity<List<AssignmentResponseDto>>
    getAssignmentsByClassIdAndSubject(
            @PathVariable Long classId,
            @PathVariable Long subjectId) {

        List<AssignmentResponseDto> response =
                assignmentService.getAssignmentsByClassIdAndSubject(
                        classId,
                        subjectId
                );

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET BY STATUS
    // ============================================================

    @Operation(summary = "Get assignments by status")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<AssignmentResponseDto>>
    getAssignmentsByStatus(
            @PathVariable String status) {

        List<AssignmentResponseDto> response =
                assignmentService.getAssignmentsByStatus(status);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Operation(summary = "Update assignment")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<AssignmentResponseDto> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRequestDto request) {

        AssignmentResponseDto response =
                assignmentService.updateAssignment(
                        id,
                        request
                );

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // DELETE
    // ============================================================

    @Operation(summary = "Delete assignment")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(
            @PathVariable Long id) {

        assignmentService.deleteAssignment(id);

        return ResponseEntity.noContent().build();
    }
}