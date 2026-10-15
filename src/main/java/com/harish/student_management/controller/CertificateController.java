package com.harish.student_management.controller;

import com.harish.student_management.dto.CertificateDtos.*;
import com.harish.student_management.service.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {
    private final CertificateService service;

    @PostMapping("/templates")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public Object template(@Valid @RequestBody TemplateRequest r) {
        return service.template(r);
    }

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public Response generate(@Valid @RequestBody GenerateRequest r) {
        return service.generate(r);
    }

    @GetMapping("/student/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<Response> student(@PathVariable Long id) {
        return service.student(id);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id) {
        byte[] b = service.download(id);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificate-" + id + ".txt").body(new ByteArrayResource(b));
    }
}
