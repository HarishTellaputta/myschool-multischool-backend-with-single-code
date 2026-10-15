package com.harish.student_management.controller;

import com.harish.student_management.dto.StudentBirthdayChatResponse;
import com.harish.student_management.dto.StudentBirthdayStatusRequest;
import com.harish.student_management.dto.StudentBirthdayStatusResponse;
import com.harish.student_management.dto.StudentBirthdayStatusUpdateRequest;
import com.harish.student_management.service.StudentBirthdayStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/student-birthday-status")
@RequiredArgsConstructor
public class StudentBirthdayStatusController {

    private final StudentBirthdayStatusService birthdayStatusService;

    // =========================================================
    // CREATE
    // PARENT ONLY
    // =========================================================

    @PostMapping
    public ResponseEntity<StudentBirthdayStatusResponse> create(
            Authentication authentication,
            @Valid @RequestBody StudentBirthdayStatusRequest request
    ) {

        String username = authentication.getName();

        StudentBirthdayStatusResponse response =
                birthdayStatusService.create(
                        username,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================
    // GET ALL
    // PARENT ONLY
    // =========================================================

    @GetMapping
    public ResponseEntity<List<StudentBirthdayStatusResponse>> getAll(
            Authentication authentication
    ) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                birthdayStatusService.getAll(username)
        );
    }

    // =========================================================
    // GET TODAY
    // PARENT ONLY
    // =========================================================

    @GetMapping("/today")
    public ResponseEntity<List<StudentBirthdayStatusResponse>> getToday(
            Authentication authentication,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate date
    ) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                birthdayStatusService.getToday(
                        username,
                        date
                )
        );
    }

    // =========================================================
    // GET BY ID
    // PARENT ONLY
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<StudentBirthdayStatusResponse> getById(
            Authentication authentication,
            @PathVariable Long id
    ) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                birthdayStatusService.getById(
                        username,
                        id
                )
        );
    }

    // =========================================================
    // GET STUDENT BIRTHDAY HISTORY
    // PARENT ONLY
    // =========================================================

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<StudentBirthdayStatusResponse>> getByStudent(
            Authentication authentication,
            @PathVariable Long studentId
    ) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                birthdayStatusService.getByStudent(
                        username,
                        studentId
                )
        );
    }

    // =========================================================
    // UPDATE
    // PARENT ONLY
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<StudentBirthdayStatusResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody
            StudentBirthdayStatusUpdateRequest request
    ) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                birthdayStatusService.update(
                        username,
                        id,
                        request
                )
        );
    }

    // =========================================================
    // DELETE
    // PARENT ONLY
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long id
    ) {

        String username = authentication.getName();

        birthdayStatusService.delete(
                username,
                id
        );

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // BIRTHDAY CHAT
    // ALL AUTHENTICATED USERS
    // =========================================================

    @GetMapping("/chat")
    public ResponseEntity<List<StudentBirthdayChatResponse>> getBirthdayChat(
            Authentication authentication
    ) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                birthdayStatusService.getBirthdayChat(username)
        );
    }
}