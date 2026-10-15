package com.harish.student_management.controller;

import com.harish.student_management.dto.JourneyTimelineDto;
import com.harish.student_management.service.JourneyTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.*;

@RestController
@RequestMapping("/api/v1/journey-timeline")
@RequiredArgsConstructor
public class JourneyTimelineController {
    private final JourneyTimelineService service;

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('PARENT','SCHOOL_ADMIN')")
    public JourneyTimelineDto timeline(@PathVariable Long studentId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return date == null ? service.today(studentId) : service.forDate(studentId, date);
    }
}
