
package com.harish.student_management.controller;

import com.harish.student_management.dto.ParentFeedbackRequestDto;
import com.harish.student_management.dto.ParentFeedbackResponseDto;
import com.harish.student_management.service.ParentFeedbackService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parent-feedback")
@RequiredArgsConstructor
public class ParentFeedbackController {

    private final ParentFeedbackService service;

    // Parent submits a complaint or suggestion.
    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ParentFeedbackResponseDto> create(
            @Valid @RequestBody ParentFeedbackRequestDto request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request, authentication.getName()));
    }

    // Only school admin can read submissions.
    @GetMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<ParentFeedbackResponseDto> listForAdmin() {
        return service.listForAdmin();
    }

    // Only school admin can delete an individual submission.
    @DeleteMapping("/{feedbackId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long feedbackId) {

        service.deleteForAdmin(feedbackId);
        return ResponseEntity.noContent().build();
    }
}
