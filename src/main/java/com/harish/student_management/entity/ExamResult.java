package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_results",
       uniqueConstraints = @UniqueConstraint(
               columnNames = {"exam_schedule_id", "student_id"},
               name = "uk_exam_result_schedule_student"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_schedule_id", nullable = false)
    private ExamSchedule examSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by_teacher_id")
    private Teacher markedByTeacher;

    @Column
    private Integer marksObtained;

    @Column
    private Integer maxMarks;

    @Column
    private Double percentage;

    @Column(length = 20)
    private String grade;

    @Column(name = "class_rank")
    private Integer classRank;

    @Column(name = "section_rank")
    private Integer sectionRank;

    @Column(length = 500)
    private String remarks;

    @Column(length = 20)
    private String status;

    @Column(name = "is_published")
    private Boolean isPublished;

    @Column(name = "marked_at")
    private LocalDateTime markedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
        if (isPublished == null) {
            isPublished = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
