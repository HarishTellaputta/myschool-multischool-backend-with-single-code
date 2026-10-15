package com.harish.student_management.controller;

import com.harish.student_management.dto.AvailableTeacherUserResponseDto;
import com.harish.student_management.dto.TeacherRequestDto;
import com.harish.student_management.dto.TeacherResponseDto;
import com.harish.student_management.service.TeacherService;
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
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TeacherController {

    private final TeacherService teacherService;

    @Operation(summary = "Get registered TEACHER users without teacher profile")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @GetMapping("/available-users")
    public ResponseEntity<List<AvailableTeacherUserResponseDto>>
    getAvailableTeacherUsers() {

        return ResponseEntity.ok(
                teacherService.getAvailableTeacherUsers()
        );
    }

    @Operation(summary = "Create a teacher profile")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PostMapping
    public ResponseEntity<TeacherResponseDto> createTeacher(
            @Valid @RequestBody TeacherRequestDto request) {

        TeacherResponseDto response =
                teacherService.createTeacher(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Get teacher by ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<TeacherResponseDto> getTeacherById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                teacherService.getTeacherById(id)
        );
    }

    @Operation(summary = "Get all teachers")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @GetMapping
    public ResponseEntity<List<TeacherResponseDto>> getAllTeachers() {

        return ResponseEntity.ok(
                teacherService.getAllTeachers()
        );
    }

    @Operation(summary = "Get teachers by school ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<TeacherResponseDto>>
    getTeachersBySchoolId(
            @PathVariable Long schoolId) {

        return ResponseEntity.ok(
                teacherService.getTeachersBySchoolId(schoolId)
        );
    }

    @Operation(summary = "Get teachers by status")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TeacherResponseDto>>
    getTeachersByStatus(
            @PathVariable String status) {

        return ResponseEntity.ok(
                teacherService.getTeachersByStatus(status)
        );
    }

    @Operation(summary = "Update teacher")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<TeacherResponseDto> updateTeacher(
            @PathVariable Long id,
            @Valid @RequestBody TeacherRequestDto request) {

        return ResponseEntity.ok(
                teacherService.updateTeacher(id, request)
        );
    }

    @Operation(summary = "Delete teacher")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeacher(
            @PathVariable Long id) {

        teacherService.deleteTeacher(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get teacher by user ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<TeacherResponseDto> getTeacherByUserId(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                teacherService.getTeacherByUserId(userId)
        );
    }
}