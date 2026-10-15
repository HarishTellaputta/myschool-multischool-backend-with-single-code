package com.harish.student_management.service;

import com.harish.student_management.dto.BirthdayChatMessageRequest;
import com.harish.student_management.dto.BirthdayChatMessageResponse;
import com.harish.student_management.dto.BirthdayChatMessageUpdateRequest;
import com.harish.student_management.dto.BirthdayChatReactionRequest;

import java.util.List;

public interface BirthdayChatService {

    List<BirthdayChatMessageResponse> getMessages(
            String username,
            Long studentId
    );

    BirthdayChatMessageResponse sendMessage(
            String username,
            Long studentId,
            BirthdayChatMessageRequest request
    );

    BirthdayChatMessageResponse editMessage(
            String username,
            Long messageId,
            BirthdayChatMessageUpdateRequest request
    );

    void deleteMessage(
            String username,
            Long messageId
    );

    BirthdayChatMessageResponse replyToMessage(
            String username,
            Long messageId,
            BirthdayChatMessageRequest request
    );

    BirthdayChatMessageResponse addReaction(
            String username,
            Long messageId,
            BirthdayChatReactionRequest request
    );

    void removeReaction(
            String username,
            Long messageId,
            BirthdayChatReactionRequest request
    );
}