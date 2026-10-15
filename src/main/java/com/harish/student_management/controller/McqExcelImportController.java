package com.harish.student_management.controller;

import com.harish.student_management.service.McqExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mcq-questions")
@RequiredArgsConstructor
public class McqExcelImportController {

    private final McqExcelImportService importService;

    @PostMapping("/import-excel")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public ResponseEntity<?> importExcel(
            @RequestParam("file") MultipartFile file) {

        int count =
                importService.importExcel(file);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "MCQ Excel imported successfully",
                        "importedQuestions",
                        count
                )
        );
    }
}