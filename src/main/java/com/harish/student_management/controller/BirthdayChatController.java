package com.harish.student_management.controller;

import com.harish.student_management.dto.*;
import com.harish.student_management.service.BirthdayChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/birthday-chat")
@RequiredArgsConstructor
public class BirthdayChatController {

    private final BirthdayChatService birthdayChatService;

    // =========================================================
    // SHOW CHAT
    // =========================================================

    @GetMapping("/{studentId}")
    public ResponseEntity<List<BirthdayChatMessageResponse>> getMessages(
            Authentication authentication,
            @PathVariable Long studentId
    ) {

        String username =
                authentication.getName();

        return ResponseEntity.ok(
                birthdayChatService.getMessages(
                        username,
                        studentId
                )
        );
    }

    // =========================================================
    // SEND MESSAGE
    // =========================================================

    @PostMapping("/{studentId}/messages")
    public ResponseEntity<BirthdayChatMessageResponse> sendMessage(
            Authentication authentication,
            @PathVariable Long studentId,
            @Valid @RequestBody BirthdayChatMessageRequest request
    ) {

        String username =
                authentication.getName();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        birthdayChatService.sendMessage(
                                username,
                                studentId,
                                request
                        )
                );
    }

    // =========================================================
    // EDIT MESSAGE
    // =========================================================

    @PutMapping("/messages/{messageId}")
    public ResponseEntity<BirthdayChatMessageResponse> editMessage(
            Authentication authentication,
            @PathVariable Long messageId,
            @Valid @RequestBody
            BirthdayChatMessageUpdateRequest request
    ) {

        String username =
                authentication.getName();

        return ResponseEntity.ok(
                birthdayChatService.editMessage(
                        username,
                        messageId,
                        request
                )
        );
    }

    // =========================================================
    // DELETE MESSAGE
    // =========================================================

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            Authentication authentication,
            @PathVariable Long messageId
    ) {

        String username =
                authentication.getName();

        birthdayChatService.deleteMessage(
                username,
                messageId
        );

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // REPLY
    // =========================================================

    @PostMapping("/messages/{messageId}/reply")
    public ResponseEntity<BirthdayChatMessageResponse> reply(
            Authentication authentication,
            @PathVariable Long messageId,
            @Valid @RequestBody
            BirthdayChatMessageRequest request
    ) {

        String username =
                authentication.getName();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        birthdayChatService.replyToMessage(
                                username,
                                messageId,
                                request
                        )
                );
    }

    // =========================================================
    // ADD REACTION
    // =========================================================

    @PostMapping("/messages/{messageId}/reaction")
    public ResponseEntity<BirthdayChatMessageResponse> addReaction(
            Authentication authentication,
            @PathVariable Long messageId,
            @Valid @RequestBody
            BirthdayChatReactionRequest request
    ) {

        String username =
                authentication.getName();

        return ResponseEntity.ok(
                birthdayChatService.addReaction(
                        username,
                        messageId,
                        request
                )
        );
    }

    // =========================================================
    // REMOVE REACTION
    // =========================================================

    @DeleteMapping("/messages/{messageId}/reaction")
    public ResponseEntity<Void> removeReaction(
            Authentication authentication,
            @PathVariable Long messageId,
            @Valid @RequestBody
            BirthdayChatReactionRequest request
    ) {

        String username =
                authentication.getName();

        birthdayChatService.removeReaction(
                username,
                messageId,
                request
        );

        return ResponseEntity.noContent().build();
    }
}