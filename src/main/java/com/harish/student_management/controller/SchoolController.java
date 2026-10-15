package com.harish.student_management.controller;

import com.harish.student_management.dto.SchoolImportResponseDto;
import com.harish.student_management.dto.SchoolRequestDto;
import com.harish.student_management.dto.SchoolResponseDto;
import com.harish.student_management.service.SchoolService;
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
@RequestMapping("/api/v1/schools")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class SchoolController {

    private final SchoolService schoolService;

    @Operation(summary = "Create a new school")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PostMapping
    public ResponseEntity<SchoolResponseDto> createSchool(
            @Valid @RequestBody SchoolRequestDto request) {
        SchoolResponseDto response = schoolService.createSchool(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get school by ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<SchoolResponseDto> getSchoolById(@PathVariable Long id) {
        SchoolResponseDto response = schoolService.getSchoolById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all schools")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @GetMapping
    public ResponseEntity<List<SchoolResponseDto>> getAllSchools() {
        List<SchoolResponseDto> response = schoolService.getAllSchools();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update school")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<SchoolResponseDto> updateSchool(
            @PathVariable Long id,
            @Valid @RequestBody SchoolRequestDto request) {
        SchoolResponseDto response = schoolService.updateSchool(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete school")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchool(@PathVariable Long id) {
        schoolService.deleteSchool(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update school status")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<SchoolResponseDto> updateSchoolStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        SchoolRequestDto request = SchoolRequestDto.builder().status(status).build();
        SchoolResponseDto response = schoolService.updateSchool(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Upload school logo")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SchoolResponseDto> uploadSchoolLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        SchoolResponseDto response = schoolService.uploadSchoolLogo(id, file);
        return ResponseEntity.ok(response);
    }


    // =========================================================
    // IMPORT SCHOOL EXCEL
    // =========================================================

    @Operation(summary = "Import schools from Excel")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<SchoolImportResponseDto> importSchools(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        SchoolImportResponseDto response =
                schoolService.importSchools(file);

        return ResponseEntity.ok(response);
    }

}
