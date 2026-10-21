package com.harish.student_management.controller;

import com.harish.student_management.dto.AcademicYearImportResponseDto;
import com.harish.student_management.dto.AcademicYearRequestDto;
import com.harish.student_management.dto.AcademicYearResponseDto;
import com.harish.student_management.service.AcademicYearExcelImportService;
import com.harish.student_management.service.AcademicYearService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-years")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AcademicYearController {

    private final AcademicYearService academicYearService;
    private final AcademicYearExcelImportService academicYearExcelImportService;

    @Operation(summary = "Create a new academic year")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PostMapping
    public ResponseEntity<AcademicYearResponseDto> createAcademicYear(
            @Valid @RequestBody AcademicYearRequestDto request) {
        AcademicYearResponseDto response = academicYearService.createAcademicYear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get academic year by ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<AcademicYearResponseDto> getAcademicYearById(@PathVariable Long id) {
        AcademicYearResponseDto response = academicYearService.getAcademicYearById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all academic years")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @GetMapping
    public ResponseEntity<List<AcademicYearResponseDto>> getAllAcademicYears() {
        List<AcademicYearResponseDto> response = academicYearService.getAllAcademicYears();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get current active academic year")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @GetMapping("/current")
    public ResponseEntity<AcademicYearResponseDto> getCurrentAcademicYear() {
        return academicYearService.getCurrentAcademicYear()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update academic year")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<AcademicYearResponseDto> updateAcademicYear(
            @PathVariable Long id,
            @Valid @RequestBody AcademicYearRequestDto request) {
        AcademicYearResponseDto response = academicYearService.updateAcademicYear(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Activate academic year")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<AcademicYearResponseDto> activateAcademicYear(
            @PathVariable Long id) {

        AcademicYearResponseDto response =
                academicYearService.setAcademicYearCurrent(id, true);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Deactivate academic year")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<AcademicYearResponseDto> deactivateAcademicYear(
            @PathVariable Long id) {

        AcademicYearResponseDto response =
                academicYearService.setAcademicYearCurrent(id, false);

        return ResponseEntity.ok(response);
    }
    @Operation(summary = "Delete academic year")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAcademicYear(@PathVariable Long id) {
        academicYearService.deleteAcademicYear(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/import")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<AcademicYearImportResponseDto> importAcademicYearsExcel(
            @RequestParam("file") MultipartFile file) {

        AcademicYearImportResponseDto response =
                academicYearExcelImportService.importAcademicYearsExcel(file);

        return ResponseEntity.ok(response);
    }
}
