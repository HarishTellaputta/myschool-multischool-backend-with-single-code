
        package com.harish.student_management.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventImportErrorDto {

    private int row;
    private String message;
}

