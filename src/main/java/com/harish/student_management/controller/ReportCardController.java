package com.harish.student_management.controller;

import com.harish.student_management.dto.ReportCardDto;
import com.harish.student_management.service.ReportCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/report-cards")
@RequiredArgsConstructor
public class ReportCardController {
    private final ReportCardService reportCardService;

    @GetMapping("/{studentId}/{examinationId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER','PARENT','STUDENT')")
    public ReportCardDto view(
            @PathVariable Long studentId,
            @PathVariable Long examinationId) {

        return reportCardService.generate(
                studentId,
                examinationId,
                false
        );
    }

    @GetMapping("/{studentId}/{examinationId}/download")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER','PARENT','STUDENT')")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long studentId, @PathVariable Long examinationId, Authentication a) {
        byte[] bytes = reportCardService.download(studentId, examinationId, isAdmin(a));
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-card-" + studentId + "-" + examinationId + ".txt").contentLength(bytes.length).body(new ByteArrayResource(bytes));
    }

    private boolean isAdmin(Authentication a) {
        return a != null && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_SCHOOL_ADMIN"));
    }
}
