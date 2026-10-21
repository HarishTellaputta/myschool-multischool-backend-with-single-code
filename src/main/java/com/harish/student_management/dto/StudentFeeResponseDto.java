
package com.harish.student_management.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFeeResponseDto {

    private Long id;
    private Long studentId;
    private Long feeStructureId;

    private String studentName;
    private String feeName;
    private String status;

    // Original fee and payment details
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;

    // Discount details
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private String discountReason;

    // Amount after discount
    private BigDecimal payableAmount;

    private LocalDate dueDate;
}
