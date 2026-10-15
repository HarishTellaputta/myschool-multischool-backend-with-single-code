package com.harish.student_management.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeStructureImportErrorDto {

    private int row;
    private String message;
}