package com.harish.student_management.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BirthdayChatMessageResponse {

    private Long id;

    private Long studentId;

    private Long senderId;

    private String senderName;

    private String message;

    private boolean edited;

    private boolean deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long replyToMessageId;

    private String replyToMessage;

    private List<BirthdayChatReactionResponse> reactions;
}