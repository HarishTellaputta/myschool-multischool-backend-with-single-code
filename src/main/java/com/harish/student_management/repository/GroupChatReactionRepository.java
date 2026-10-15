
package com.harish.student_management.repository;

import com.harish.student_management.entity.GroupChatReaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GroupChatReactionRepository
        extends JpaRepository<GroupChatReaction, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<GroupChatReaction> findByMessage_IdIn(
            Collection<Long> messageIds
    );

    boolean existsByMessage_IdAndUser_IdAndReaction(
            Long messageId,
            Long userId,
            String reaction
    );

    void deleteByMessage_IdAndUser_IdAndReaction(
            Long messageId,
            Long userId,
            String reaction
    );
}
