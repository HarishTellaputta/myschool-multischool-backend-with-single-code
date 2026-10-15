
        package com.harish.student_management.service;

import com.harish.student_management.dto.*;

import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.util.*;

public interface CalendarEventService {

    CalendarEventResponseDto create(CalendarEventRequestDto r);

    CalendarEventResponseDto update(Long id, CalendarEventRequestDto r);

    void delete(Long id);

    List<CalendarEventResponseDto> list(
            LocalDate from,
            LocalDate to,
            String audience,
            Long classId
    );

    CalendarEventImportResponseDto importExcel(MultipartFile file);
}

