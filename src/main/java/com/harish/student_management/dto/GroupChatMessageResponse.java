
package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupChatMessageResponse {

    private Long id;

    private Long senderId;
    private String senderName;
    private String senderRole;

    private String message;

    private boolean edited;
    private boolean deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long replyToMessageId;
    private String replyToMessage;
    private String replyToSenderName;

    private List<GroupChatReactionResponse> reactions = new ArrayList<>();
}
