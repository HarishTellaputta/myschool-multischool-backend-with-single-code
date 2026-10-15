package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableImportResponseDto {

    private int totalRows;
    private int successCount;
    private int failedCount;

    private List<TimetableImportErrorDto> errors;
}