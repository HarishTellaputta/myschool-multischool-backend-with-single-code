
package com.harish.student_management.controller;

import com.harish.student_management.dto.GroupChatMessageRequest;
import com.harish.student_management.dto.GroupChatMessageResponse;
import com.harish.student_management.dto.GroupChatReactionRequest;
import com.harish.student_management.service.GroupChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group-chat")
public class GroupChatController {

    private final GroupChatService groupChatService;

    public GroupChatController(GroupChatService groupChatService) {
        this.groupChatService = groupChatService;
    }

    @GetMapping("/messages")
    public ResponseEntity<List<GroupChatMessageResponse>> getMessages() {
        return ResponseEntity.ok(groupChatService.getMessages());
    }

    @PostMapping("/messages")
    public ResponseEntity<GroupChatMessageResponse> sendMessage(
            @Valid @RequestBody GroupChatMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupChatService.sendMessage(request));
    }

    @PutMapping("/messages/{messageId}")
    public ResponseEntity<GroupChatMessageResponse> editMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody GroupChatMessageRequest request
    ) {
        return ResponseEntity.ok(
                groupChatService.editMessage(messageId, request)
        );
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId
    ) {
        groupChatService.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/messages/{messageId}/reactions")
    public ResponseEntity<GroupChatMessageResponse> addReaction(
            @PathVariable Long messageId,
            @Valid @RequestBody GroupChatReactionRequest request
    ) {
        return ResponseEntity.ok(
                groupChatService.addReaction(messageId, request)
        );
    }

    @DeleteMapping("/messages/{messageId}/reactions")
    public ResponseEntity<Void> removeReaction(
            @PathVariable Long messageId,
            @Valid @RequestBody GroupChatReactionRequest request
    ) {
        groupChatService.removeReaction(messageId, request);
        return ResponseEntity.noContent().build();
    }
}
