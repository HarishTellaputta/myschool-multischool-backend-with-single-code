
package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "group_chat_reactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_group_chat_message_user_reaction",
                columnNames = {"message_id", "user_id", "reaction"}
        ),
        indexes = @Index(
                name = "idx_group_chat_reaction_message_id",
                columnList = "message_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class GroupChatReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private GroupChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String reaction;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
