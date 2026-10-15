package com.harish.student_management.controller;

import com.harish.student_management.dto.*;
import com.harish.student_management.service.CommunicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/communications")
@RequiredArgsConstructor
public class CommunicationController {

    private final CommunicationService service;

    /*
     * ============================================================
     * START CONVERSATION
     *
     * Parent OR Teacher
     * ============================================================
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('PARENT','TEACHER')")
    public ResponseEntity<ConversationResponseDto> initiate(
            @Valid @RequestBody ConversationRequestDto r,
            Authentication a
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.initiate(
                                r,
                                a.getName()
                        )
                );
    }

    /*
     * ============================================================
     * TEACHER'S STUDENTS FOR NEW CHAT
     * ============================================================
     */
    @GetMapping("/teacher/students")
    @PreAuthorize("hasRole('TEACHER')")
    public List<StudentChatResponseDto> teacherStudents(
            Authentication a
    ) {

        return service.getTeacherStudents(
                a.getName()
        );
    }

    /*
     * ============================================================
     * SEND MESSAGE
     * ============================================================
     */
    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAnyRole('PARENT','TEACHER')")
    public ResponseEntity<MessageResponseDto> message(
            @PathVariable Long id,
            @Valid @RequestBody MessageRequestDto r,
            Authentication a
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.reply(
                                id,
                                a.getName(),
                                r
                        )
                );
    }

    /*
     * ============================================================
     * CONVERSATION HISTORY
     * ============================================================
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARENT','TEACHER')")
    public ConversationResponseDto history(
            @PathVariable Long id,
            Authentication a
    ) {

        return service.history(
                id,
                a.getName()
        );
    }

    /*
     * ============================================================
     * MY CONVERSATIONS
     * ============================================================
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT','TEACHER')")
    public List<ConversationResponseDto> list(
            Authentication a
    ) {

        return service.conversations(
                a.getName()
        );
    }
}