package com.harish.student_management.controller;

import com.harish.student_management.dto.AdminDashboardDto;
import com.harish.student_management.service.AdminDashboardService;
import com.harish.student_management.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService service;
    private final ExcelImportService excelImportService;

    @GetMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public AdminDashboardDto dashboard() {
        return service.get();
    }

//    @PostMapping(
//            value = "/import-excel",
//            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
//    )
//    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
//    public ResponseEntity<String> importExcel(
//            @RequestParam("file") MultipartFile file) {
//
//        return ResponseEntity.ok(
//                excelImportService.importStudentsAndParents(file)
//        );
//    }
}