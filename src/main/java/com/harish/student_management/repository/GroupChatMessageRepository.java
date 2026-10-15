
package com.harish.student_management.repository;

import com.harish.student_management.entity.GroupChatMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupChatMessageRepository
        extends JpaRepository<GroupChatMessage, Long> {

    @EntityGraph(attributePaths = {"sender"})
    @Query("""
        SELECT m
        FROM GroupChatMessage m
        ORDER BY m.createdAt ASC
        """)
    List<GroupChatMessage> findAllForGroupChat();

    @Query("""
        SELECT m
        FROM GroupChatMessage m
        JOIN FETCH m.sender
        LEFT JOIN FETCH m.replyToMessage r
        LEFT JOIN FETCH r.sender
        WHERE m.id = :messageId
        """)
    Optional<GroupChatMessage> findByIdWithDetails(
            @Param("messageId") Long messageId
    );
}
