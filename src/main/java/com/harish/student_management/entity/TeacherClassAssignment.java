package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "teacher_class_assignments",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "teacher_id",
                                "class_id",
                                "section_id",
                                "subject_id"
                        },
                        name = "uk_teacher_class_section_subject"
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherClassAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "teacher_id",
            nullable = false
    )
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "class_id",
            nullable = false
    )
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "subject_id",
            nullable = false
    )
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    @Column(
            name = "assigned_at",
            updatable = false
    )
    private LocalDateTime assignedAt;

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }
}