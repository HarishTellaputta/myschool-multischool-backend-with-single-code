package com.harish.student_management.controller;

import com.harish.student_management.dto.*;
import com.harish.student_management.service.CalendarEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/calendar-events")
@RequiredArgsConstructor
public class CalendarEventController {
    private final CalendarEventService service;

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<CalendarEventResponseDto> create(@Valid @RequestBody CalendarEventRequestDto r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CalendarEventResponseDto update(@PathVariable Long id, @Valid @RequestBody CalendarEventRequestDto r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PARENT','TEACHER','STUDENT')")
    public List<CalendarEventResponseDto> list(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, @RequestParam(required = false) Long classId, Authentication a) {
        return service.list(from, to, audience(a), classId);
    }

    private String audience(Authentication a) {
        if (a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_SCHOOL_ADMIN"))) return null;
        if (a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_PARENT"))) return "PARENTS";
        if (a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_TEACHER"))) return "TEACHERS";
        return "STUDENTS";
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<?> importExcel(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(service.importExcel(file));
    }
}
