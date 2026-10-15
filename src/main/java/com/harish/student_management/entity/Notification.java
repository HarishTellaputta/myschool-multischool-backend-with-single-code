package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.*;
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id")
    private User recipientUser;

    @Column(name = "recipient_role", length = 30)
    private String recipientRole;

    @Column(name = "recipient_id")
    private Long recipientId;

    @Column(name = "is_read", nullable = false)
    private Boolean read;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // =========================
    // ADMIN NOTICE FIELDS
    // =========================

    @Column(length = 30)
    private String category;

    @Column(length = 50)
    private String audience;

    @Column(length = 20)
    private String status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @PrePersist
    void create() {
        createdAt = LocalDateTime.now();

        if (read == null) {
            read = false;
        }

        if (status == null) {
            status = "DRAFT";
        }
    }
}