package com.harish.student_management.controller;

import com.harish.student_management.dto.SubjectImportResponseDto;
import com.harish.student_management.dto.SubjectRequest;
import com.harish.student_management.dto.SubjectResponse;
import com.harish.student_management.service.SubjectService;
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
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<SubjectResponse> createSubject(
            @Valid @RequestBody SubjectRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subjectService.createSubject(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<List<SubjectResponse>> getAllSubjects() {
        return ResponseEntity.ok(subjectService.getAllSubjects());
    }

    @GetMapping("/school/{schoolId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<List<SubjectResponse>> getSubjectsBySchool(
            @PathVariable Long schoolId) {
        return ResponseEntity.ok(
                subjectService.getSubjectsBySchool(schoolId));
    }

    @GetMapping("/school/{schoolId}/active")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<List<SubjectResponse>> getActiveSubjects(
            @PathVariable Long schoolId) {
        return ResponseEntity.ok(
                subjectService.getActiveSubjectsBySchool(schoolId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<SubjectResponse> getSubject(
            @PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getSubjectById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<SubjectResponse> updateSubject(
            @PathVariable Long id,
            @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.updateSubject(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<List<SubjectResponse>> getSubjectsByClass(
            @PathVariable Long classId) {
        return ResponseEntity.ok(subjectService.getSubjectsByClass(classId));
    }

    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<SubjectImportResponseDto> importSubjects(
            @RequestParam("file") MultipartFile file,
            @RequestParam("schoolId") Long schoolId) throws IOException {

        return ResponseEntity.ok(
                subjectService.importSubjects(file, schoolId));
    }
}