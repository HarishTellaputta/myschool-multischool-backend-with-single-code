
        package com.harish.student_management.controller;

import com.harish.student_management.dto.HomeworkRequestDto;
import com.harish.student_management.dto.HomeworkResponseDto;
import com.harish.student_management.service.HomeworkService;
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
@RequestMapping("/api/v1/homeworks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class HomeworkController {

    private final HomeworkService homeworkService;

    @Operation(summary = "Create homework")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PostMapping
    public ResponseEntity<HomeworkResponseDto> createHomework(
            @Valid @RequestBody HomeworkRequestDto request) {

        HomeworkResponseDto response =
                homeworkService.createHomework(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Get homework by ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<HomeworkResponseDto> getHomeworkById(
            @PathVariable Long id) {

        HomeworkResponseDto response =
                homeworkService.getHomeworkById(id);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all homework")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @GetMapping
    public ResponseEntity<List<HomeworkResponseDto>> getAllHomework() {

        List<HomeworkResponseDto> response =
                homeworkService.getAllHomework();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get homework by class ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @GetMapping("/class/{classId}")
    public ResponseEntity<List<HomeworkResponseDto>> getHomeworkByClassId(
            @PathVariable Long classId) {

        List<HomeworkResponseDto> response =
                homeworkService.getHomeworkByClassId(classId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get homework by section ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @GetMapping("/section/{sectionId}")
    public ResponseEntity<List<HomeworkResponseDto>> getHomeworkBySectionId(
            @PathVariable Long sectionId) {

        List<HomeworkResponseDto> response =
                homeworkService.getHomeworkBySectionId(sectionId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get homework by class ID and section ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @GetMapping("/class/{classId}/section/{sectionId}")
    public ResponseEntity<List<HomeworkResponseDto>> getHomeworkByClassIdAndSectionId(
            @PathVariable Long classId,
            @PathVariable Long sectionId) {

        List<HomeworkResponseDto> response =
                homeworkService.getHomeworkByClassIdAndSectionId(
                        classId,
                        sectionId
                );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get homework by teacher ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<HomeworkResponseDto>> getHomeworkByTeacherId(
            @PathVariable Long teacherId) {

        List<HomeworkResponseDto> response =
                homeworkService.getHomeworkByTeacherId(teacherId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get homework by subject ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<List<HomeworkResponseDto>> getHomeworkBySubject(
            @PathVariable Long subjectId) {

        List<HomeworkResponseDto> response =
                homeworkService.getHomeworkBySubject(subjectId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get homework by class ID and due date range")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @GetMapping("/class/{classId}/date-range")
    public ResponseEntity<List<HomeworkResponseDto>> getHomeworkByClassIdAndDueDateRange(
            @PathVariable Long classId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        List<HomeworkResponseDto> response =
                homeworkService.getHomeworkByClassIdAndDueDateRange(
                        classId,
                        startDate,
                        endDate
                );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get active homework by class ID (not expired)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @GetMapping("/class/{classId}/active")
    public ResponseEntity<List<HomeworkResponseDto>> getActiveHomeworkByClassId(
            @PathVariable Long classId) {

        List<HomeworkResponseDto> response =
                homeworkService.getActiveHomeworkByClassId(classId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get homework by class ID and subject ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @GetMapping("/class/{classId}/subject/{subjectId}")
    public ResponseEntity<List<HomeworkResponseDto>> getHomeworkByClassIdAndSubject(
            @PathVariable Long classId,
            @PathVariable Long subjectId) {

        List<HomeworkResponseDto> response =
                homeworkService.getHomeworkByClassIdAndSubject(
                        classId,
                        subjectId
                );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update homework")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<HomeworkResponseDto> updateHomework(
            @PathVariable Long id,
            @Valid @RequestBody HomeworkRequestDto request) {

        HomeworkResponseDto response =
                homeworkService.updateHomework(id, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete homework")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHomework(
            @PathVariable Long id) {

        homeworkService.deleteHomework(id);

        return ResponseEntity.noContent().build();
    }
}

