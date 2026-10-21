
package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "attendance_periods",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attendance_period_student_class_date_period",
                        columnNames = {
                                "student_id",
                                "class_id",
                                "attendance_date",
                                "period_number"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_attendance_period_class_date",
                        columnList = "class_id, attendance_date"
                ),
                @Index(
                        name = "idx_attendance_period_student_date",
                        columnList = "student_id, attendance_date"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendancePeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by_teacher_id")
    private Teacher markedByTeacher;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String remarks;

    @Column(name = "marked_at", nullable = false)
    private LocalDateTime markedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_teacher_id")
    private Teacher updatedByTeacher;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (markedAt == null) {
            markedAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
