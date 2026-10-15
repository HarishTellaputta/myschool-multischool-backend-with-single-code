package com.harish.student_management.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.*;

@Entity
@Table(name = "fee_structures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
 @ManyToOne(fetch = FetchType.LAZY)
 @JoinColumn(name = "class_id", nullable = false)
 @JsonIgnore
 private ClassEntity classEntity;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    @JsonIgnore
    private AcademicYear academicYear;
    @Column(nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal amount;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    @Column(length = 20)
    private String status;
    @Column(name = "is_applied", nullable = false)
    @Builder.Default
    private boolean applied = false;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void create() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
    }
}
