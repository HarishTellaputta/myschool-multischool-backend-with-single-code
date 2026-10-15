package com.harish.student_management.controller;

import com.harish.student_management.dto.*;
import com.harish.student_management.service.SchoolNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class SchoolNoticeController {
    private final SchoolNoticeService service;

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<SchoolNoticeResponseDto> create(@Valid @RequestBody SchoolNoticeRequestDto r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolNoticeResponseDto update(@PathVariable Long id, @Valid @RequestBody SchoolNoticeRequestDto r) {
        return service.update(id, r);
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolNoticeResponseDto publish(@PathVariable Long id, @RequestParam boolean published) {
        return service.publish(id, published);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PARENT','TEACHER','STUDENT')")
    public List<SchoolNoticeResponseDto> relevant(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            Authentication a
    ) {
        String role = a.getAuthorities()
                .stream()
                .map(x -> x.getAuthority())
                .filter(x -> x.startsWith("ROLE_"))
                .findFirst()
                .orElse("");

        System.out.println("========================================");
        System.out.println("NOTICE GET USER AUTHORITIES: " + a.getAuthorities());
        System.out.println("NOTICE GET ROLE: " + role);
        System.out.println("CLASS ID: " + classId);
        System.out.println("SECTION ID: " + sectionId);
        System.out.println("========================================");

        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        return service.relevant(role, classId, sectionId);
    }
}
