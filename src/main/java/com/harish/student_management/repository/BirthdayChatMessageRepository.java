package com.harish.student_management.repository;

import com.harish.student_management.entity.BirthdayChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BirthdayChatMessageRepository
        extends JpaRepository<BirthdayChatMessage, Long> {

    @Query("""
        SELECT m
        FROM BirthdayChatMessage m
        JOIN FETCH m.sender
        LEFT JOIN FETCH m.replyToMessage
        WHERE m.student.id = :studentId
        ORDER BY m.createdAt ASC
    """)
    List<BirthdayChatMessage> findMessagesByStudentId(
            @Param("studentId") Long studentId
    );

    @Query("""
        SELECT m
        FROM BirthdayChatMessage m
        JOIN FETCH m.student s
        JOIN FETCH m.sender
        WHERE m.id = :messageId
          AND s.parent.id = :parentId
    """)
    Optional<BirthdayChatMessage> findByIdAndParentId(
            @Param("messageId") Long messageId,
            @Param("parentId") Long parentId
    );
}