package com.harish.student_management.controller;

import com.harish.student_management.dto.McqQuestionRequestDto;
import com.harish.student_management.dto.McqQuestionResponseDto;
import com.harish.student_management.service.McqQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/mcq-questions")
@RequiredArgsConstructor
public class McqQuestionController {

    private final McqQuestionService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public ResponseEntity<McqQuestionResponseDto> create(
            @Valid @RequestBody McqQuestionRequestDto request,
            Authentication authentication) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.create(
                                request,
                                authentication.getName(),
                                isAdmin(authentication)
                        )
                );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public McqQuestionResponseDto get(
            @PathVariable Long id) {

        return service.get(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public List<McqQuestionResponseDto> list(
            @RequestParam(required = false)
            LocalDate date,

            @RequestParam(required = false)
            Long classId,

            @RequestParam(required = false)
            Long subjectId) {

        return service.list(
                date,
                classId,
                subjectId
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public McqQuestionResponseDto update(
            @PathVariable Long id,
            @Valid @RequestBody McqQuestionRequestDto request,
            Authentication authentication) {

        return service.update(
                id,
                request,
                authentication.getName(),
                isAdmin(authentication)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication) {

        service.delete(
                id,
                authentication.getName(),
                isAdmin(authentication)
        );

        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {

        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority
                                .getAuthority()
                                .equals("ROLE_SCHOOL_ADMIN")
                );
    }
}