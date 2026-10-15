package com.harish.student_management.service;

import com.harish.student_management.dto.*;

import java.util.List;

public interface CommunicationService {

    ConversationResponseDto initiate(
            ConversationRequestDto request,
            String username
    );

    MessageResponseDto reply(
            Long conversationId,
            String username,
            MessageRequestDto request
    );

    ConversationResponseDto history(
            Long id,
            String username
    );

    List<ConversationResponseDto> conversations(
            String username
    );

    List<StudentChatResponseDto> getTeacherStudents(
            String username
    );
}