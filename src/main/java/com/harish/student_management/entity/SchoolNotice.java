package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.*;

@Entity
@Table(name = "school_notices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolNotice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, length = 200)
    String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    String content;
    @Column(nullable = false, length = 30)
    String audience;
    @Column(length=30)
    String category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    ClassEntity classEntity;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    Section section;
    @Column(name = "published_at")
    LocalDateTime publishedAt;
    @Column(length = 20)
    String status;
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    void create() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "DRAFT";
    }
}
