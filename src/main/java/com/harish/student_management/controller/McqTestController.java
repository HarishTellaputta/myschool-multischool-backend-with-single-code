package com.harish.student_management.controller;

import com.harish.student_management.dto.*;
import com.harish.student_management.service.McqTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/mcq-tests")
@RequiredArgsConstructor
public class McqTestController {
    private final McqTestService service;

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<McqTestResponseDto> create(@Valid @RequestBody McqTestRequestDto r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER','PARENT','STUDENT')")
    public List<McqTestResponseDto> list(@RequestParam(required = false) LocalDate date) {
        return service.list(date);
    }

    @PostMapping("/{testId}/start")
    @PreAuthorize("hasAnyRole('STUDENT','PARENT')")
    public McqAttemptResponseDto start(
            @PathVariable Long testId,
            @RequestParam Long studentId,
            Authentication authentication) {

        boolean isParent = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT"));

        return service.start(
                testId,
                studentId,
                authentication.getName(),
                isParent
        );
    }

    @PutMapping("/attempts/{attemptId}/answers")
    @PreAuthorize("hasAnyRole('STUDENT','PARENT')")
    public McqAttemptResponseDto answer(
            @PathVariable Long attemptId,
            @RequestParam Long studentId,
            @Valid @RequestBody McqAnswerRequestDto r,
            Authentication authentication) {

        boolean isParent = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT"));

        return service.answer(
                attemptId,
                studentId,
                r,
                authentication.getName(),
                isParent
        );
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @PreAuthorize("hasAnyRole('STUDENT','PARENT')")
    public McqAttemptResponseDto submit(
            @PathVariable Long attemptId,
            @RequestParam Long studentId,
            Authentication authentication) {

        boolean isParent = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT"));

        return service.submit(
                attemptId,
                studentId,
                authentication.getName(),
                isParent
        );
    }

    @GetMapping("/history/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT','PARENT','SCHOOL_ADMIN')")
    public List<McqAttemptResponseDto> history(@PathVariable Long studentId) {
        return service.history(studentId);
    }

    @GetMapping("/performance/class/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER','SCHOOL_ADMIN')")
    public List<McqAttemptResponseDto> classPerformance(@PathVariable Long classId) {
        return service.classPerformance(classId);
    }

    @GetMapping("/performance/overall")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<McqAttemptResponseDto> overallPerformance() {
        return service.overallPerformance();
    }




}
