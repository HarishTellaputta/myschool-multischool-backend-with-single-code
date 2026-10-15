package com.harish.student_management.repository;

import com.harish.student_management.entity.CommunicationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunicationMessageRepository
        extends JpaRepository<CommunicationMessage, Long> {

    List<CommunicationMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    long countByConversationIdAndReadFalseAndSenderRoleNot(
            Long conversationId,
            String role
    );

    long countByConversationIdAndReadFalseAndSenderRole(
            Long conversationId,
            String role
    );
}