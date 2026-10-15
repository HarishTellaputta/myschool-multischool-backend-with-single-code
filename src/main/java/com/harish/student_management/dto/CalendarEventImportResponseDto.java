
        package com.harish.student_management.dto;

import lombok.*;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventImportResponseDto {

    private int totalRows;
    private int created;
    private int updated;
    private int errors;

    private List<CalendarEventImportErrorDto> errorDetails;
}


