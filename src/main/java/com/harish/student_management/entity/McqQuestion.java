package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mcq_questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McqQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_teacher_id")
    private Teacher createdByTeacher;

    @Column(name = "question_date", nullable = false)
    private LocalDate questionDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    // Question image - optional
    @Column(name = "question_image_url", length = 500)
    private String questionImageUrl;

    @Column(name = "option_a", nullable = false, length = 500)
    private String optionA;

    @Column(name = "option_a_image_url", length = 500)
    private String optionAImageUrl;

    @Column(name = "option_b", nullable = false, length = 500)
    private String optionB;

    @Column(name = "option_b_image_url", length = 500)
    private String optionBImageUrl;

    @Column(name = "option_c", nullable = false, length = 500)
    private String optionC;

    @Column(name = "option_c_image_url", length = 500)
    private String optionCImageUrl;

    @Column(name = "option_d", nullable = false, length = 500)
    private String optionD;

    @Column(name = "option_d_image_url", length = 500)
    private String optionDImageUrl;

    @Column(name = "correct_answer", nullable = false, length = 1)
    private String correctAnswer;

    @Column(nullable = false)
    private Integer marks;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;

        if (questionDate == null) {
            questionDate = LocalDate.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}