package com.harish.student_management.controller;

import com.harish.student_management.dto.*;
import com.harish.student_management.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    // =========================
    // ADMIN NOTICE APIs
    // =========================

    @GetMapping("/admin")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<NotificationResponseDto> adminNotices() {
        return service.adminNotices();
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<NotificationResponseDto> createNotice(
            @Valid @RequestBody NotificationRequestDto request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createNotice(request));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public NotificationResponseDto updateNotice(
            @PathVariable Long id,
            @RequestBody NotificationRequestDto request
    ) {

        return service.updateNotice(id, request);
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<Void> deleteNotice(
            @PathVariable Long id
    ) {

        service.deleteNotice(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/{id}/publish")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public NotificationResponseDto publishNotice(
            @PathVariable Long id
    ) {

        return service.publishNotice(id);
    }


    // =========================
    // EXISTING USER APIs
    // =========================

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<Void> publish(
            @Valid @RequestBody NotificationRequestDto r
    ) {
        service.publish(r);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<NotificationResponseDto> mine(Authentication a) {
        return service.forUser(a.getName());
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public long unread(Authentication a) {
        return service.unread(a.getName());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public NotificationResponseDto get(
            @PathVariable Long id,
            Authentication a
    ) {
        return service.get(id, a.getName());
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public NotificationResponseDto read(
            @PathVariable Long id,
            Authentication a
    ) {
        return service.markRead(id, a.getName());
    }
}