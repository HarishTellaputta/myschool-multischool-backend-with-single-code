package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectImportResponseDto {

    private int totalRows;

    private int createdCount;

    private int updatedCount;

    private int failedCount;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    private String message;
}