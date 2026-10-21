package com.harish.student_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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

    @Column(name = "discount_type", length = 20)
    @Builder.Default
    private String discountType = "NONE";

    @Column(name = "discount_value", nullable = false,
            precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false,
            precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_reason", length = 255)
    private String discountReason;

    @Column(name = "discount_approved_by", length = 100)
    private String discountApprovedBy;

    @PrePersist
    void create() {
        if (paidAmount == null) paidAmount = java.math.BigDecimal.ZERO;
        if (status == null) status = "PENDING";
    }
}
