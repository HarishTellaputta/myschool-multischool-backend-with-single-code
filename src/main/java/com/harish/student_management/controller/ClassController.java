package com.harish.student_management.controller;

import com.harish.student_management.dto.ClassImportResponseDto;
import com.harish.student_management.dto.ClassRequestDto;
import com.harish.student_management.dto.ClassResponseDto;
import com.harish.student_management.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ClassController {

    private final ClassService classService;

    @Operation(summary = "Create a new class")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PostMapping
    public ResponseEntity<ClassResponseDto> createClass(
            @Valid @RequestBody ClassRequestDto request) {

        ClassResponseDto response = classService.createClass(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Get class by ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<ClassResponseDto> getClassById(
            @PathVariable Long id) {

        ClassResponseDto response = classService.getClassById(id);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all classes")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @GetMapping
    public ResponseEntity<List<ClassResponseDto>> getAllClasses() {

        List<ClassResponseDto> response = classService.getAllClasses();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get classes by school ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<ClassResponseDto>> getClassesBySchoolId(
            @PathVariable Long schoolId) {

        List<ClassResponseDto> response =
                classService.getClassesBySchoolId(schoolId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update class")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<ClassResponseDto> updateClass(
            @PathVariable Long id,
            @Valid @RequestBody ClassRequestDto request) {

        ClassResponseDto response =
                classService.updateClass(id, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete class")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClass(
            @PathVariable Long id) {

        classService.deleteClass(id);

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // IMPORT CLASSES FROM EXCEL
    // =========================================================

    @Operation(summary = "Import classes from Excel")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ClassImportResponseDto> importClasses(
            @RequestParam("file") MultipartFile file,
            @RequestParam("schoolId") Long schoolId) throws IOException {

        ClassImportResponseDto response =
                classService.importClasses(file, schoolId);

        return ResponseEntity.ok(response);
    }
}