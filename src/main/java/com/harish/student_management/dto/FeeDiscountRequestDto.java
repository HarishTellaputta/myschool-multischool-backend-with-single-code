package com.harish.student_management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeeDiscountRequestDto {

    @NotBlank
    private String discountType; // NONE, FIXED, PERCENTAGE

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal discountValue;

    @Size(max = 255)
    private String discountReason;
}