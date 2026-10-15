package com.harish.student_management.controller;

import com.harish.student_management.dto.ParentRequestDto;
import com.harish.student_management.dto.ParentResponseDto;
import com.harish.student_management.service.ParentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parents")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ParentResponseDto createParent(
            @Valid @RequestBody ParentRequestDto request) {

        return parentService.createParent(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public List<ParentResponseDto> getAllParents() {

        return parentService.getAllParents();
    }

    // =========================================================
    // GET LOGGED-IN PARENT PROFILE + CHILDREN
    // =========================================================

    @GetMapping("/me")
    @PreAuthorize("hasRole('PARENT')")
    public ParentResponseDto getMyProfile(
            Authentication authentication) {

        return parentService.getMyProfile(
                authentication.getName()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ParentResponseDto getParentById(
            @PathVariable Long id) {

        return parentService.getParentById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ParentResponseDto updateParent(
            @PathVariable Long id,
            @Valid @RequestBody ParentRequestDto request) {

        return parentService.updateParent(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<String> deleteParent(
            @PathVariable Long id) {

        parentService.deleteParent(id);

        return ResponseEntity.ok(
                "Parent deleted successfully"
        );
    }

    @PostMapping("/{parentId}/students/{studentId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ParentResponseDto linkStudent(
            @PathVariable Long parentId,
            @PathVariable Long studentId) {

        return parentService.linkStudent(
                parentId,
                studentId
        );
    }

    @DeleteMapping("/{parentId}/students/{studentId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ParentResponseDto unlinkStudent(
            @PathVariable Long parentId,
            @PathVariable Long studentId) {

        return parentService.unlinkStudent(
                parentId,
                studentId
        );
    }
}