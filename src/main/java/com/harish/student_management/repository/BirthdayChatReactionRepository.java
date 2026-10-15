package com.harish.student_management.repository;


import com.harish.student_management.entity.BirthdayChatReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BirthdayChatReactionRepository
        extends JpaRepository<BirthdayChatReaction, Long> {

    List<BirthdayChatReaction> findByMessageId(Long messageId);

    Optional<BirthdayChatReaction>
    findByMessageIdAndUserIdAndReaction(
            Long messageId,
            Long userId,
            String reaction
    );

    void deleteByMessageIdAndUserIdAndReaction(
            Long messageId,
            Long userId,
            String reaction
    );
}