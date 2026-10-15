package com.harish.student_management.dto;


import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeDashboardSummaryDto {

    private BigDecimal totalFee;

    private BigDecimal collected;

    private BigDecimal pending;

    private BigDecimal collectionPercentage;
}