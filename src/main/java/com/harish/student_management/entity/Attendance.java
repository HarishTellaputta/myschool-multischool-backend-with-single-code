
package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "attendances",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "student_id",
                                "class_id",
                                "attendance_date",
                                "period_number"
                        },
                        name = "uk_student_class_date_period"
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by_teacher_id")
    private Teacher markedByTeacher;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    /**
     * 0 = daily SINGLE attendance.
     * 1..N = PERIOD_WISE attendance.
     */
    @Builder.Default
    @Column(name = "period_number", nullable = false)
    private Integer periodNumber = 0;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String remarks;

    @Column(name = "marked_at")
    private LocalDateTime markedAt;

    @Column(name = "updated_at")
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

        if (periodNumber == null) {
            periodNumber = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
