package com.harish.student_management.repository;

import com.harish.student_management.entity.CommunicationConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommunicationConversationRepository
        extends JpaRepository<CommunicationConversation, Long> {

    List<CommunicationConversation>
    findByParentIdOrderByCreatedAtDesc(Long id);

    List<CommunicationConversation>
    findByTeacherIdOrderByCreatedAtDesc(Long id);

    Optional<CommunicationConversation>
    findByParentIdAndTeacherIdAndStudentIdAndSubject(
            Long parentId,
            Long teacherId,
            Long studentId,
            String subject
    );
}