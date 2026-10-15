package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.*;

@Entity
@Table(name = "student_fees", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "fee_structure_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_structure_id", nullable = false)
    private FeeStructure feeStructure;
    @Column(nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal totalAmount;
    @Column(nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal paidAmount;
    @Column(nullable = false)
    private LocalDate dueDate;
    @Column(length = 20)
    private String status;

    @PrePersist
    void create() {
        if (paidAmount == null) paidAmount = java.math.BigDecimal.ZERO;
        if (status == null) status = "PENDING";
    }
}
