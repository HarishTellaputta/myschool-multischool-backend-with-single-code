
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
public class ExaminationImportResponseDto {

    private int totalRows;
    private int created;
    private int updated;
    private int errors;

    private List<ExaminationImportErrorDto> errorDetails;
}

