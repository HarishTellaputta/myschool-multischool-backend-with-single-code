package com.harish.student_management.dto;

import lombok.*;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeStructureImportResultDto {

    private int totalRows;
    private int created;
    private int skipped;
    private int errors;

    @Builder.Default
    private List<FeeStructureImportErrorDto> errorDetails = new ArrayList<>();
}