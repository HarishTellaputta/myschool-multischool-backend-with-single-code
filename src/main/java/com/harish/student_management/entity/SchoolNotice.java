package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "school_notices",
        indexes = {
                @Index(
                        name = "idx_school_notice_school_status",
                        columnList = "school_id, status"
                )
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 30)
    private String audience;

    @Column(length = 30)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void create() {
        createdAt = LocalDateTime.now();

        if (status == null || status.isBlank()) {
            status = "DRAFT";
        }
    }
}