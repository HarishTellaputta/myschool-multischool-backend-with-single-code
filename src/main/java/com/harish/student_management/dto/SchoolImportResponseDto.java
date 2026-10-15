package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolImportResponseDto {

    private int totalRows;
    private int createdCount;
    private int updatedCount;
    private int failedCount;

    private List<String> errors;

    private String message;
}