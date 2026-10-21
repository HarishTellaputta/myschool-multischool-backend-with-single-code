package com.harish.student_management.controller;

import com.harish.student_management.dto.SectionImportResponseDto;
import com.harish.student_management.dto.SectionRequestDto;
import com.harish.student_management.dto.SectionResponseDto;
import com.harish.student_management.service.SectionService;
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
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class SectionController {

    private final SectionService sectionService;

    @Operation(summary = "Create a new section")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PostMapping
    public ResponseEntity<SectionResponseDto> createSection(
            @Valid @RequestBody SectionRequestDto request) {

        SectionResponseDto response =
                sectionService.createSection(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Get section by ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<SectionResponseDto> getSectionById(
            @PathVariable Long id) {

        SectionResponseDto response =
                sectionService.getSectionById(id);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all sections")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @GetMapping
    public ResponseEntity<List<SectionResponseDto>> getAllSections() {

        List<SectionResponseDto> response =
                sectionService.getAllSections();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get sections by class ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @GetMapping("/class/{classId}")
    public ResponseEntity<List<SectionResponseDto>> getSectionsByClassId(
            @PathVariable Long classId) {

        List<SectionResponseDto> response =
                sectionService.getSectionsByClassId(classId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update section")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ResponseEntity<SectionResponseDto> updateSection(
            @PathVariable Long id,
            @Valid @RequestBody SectionRequestDto request) {

        SectionResponseDto response =
                sectionService.updateSection(id, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete section")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSection(
            @PathVariable Long id) {

        sectionService.deleteSection(id);

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // IMPORT SECTIONS FROM EXCEL
    // =========================================================

    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<SectionImportResponseDto> importSections(
            @RequestParam("file") MultipartFile file) throws IOException {

        SectionImportResponseDto response =
                sectionService.importSections(file);

        return ResponseEntity.ok(response);
    }
}