    package com.harish.student_management.controller;

    import com.harish.student_management.dto.CalendarEventRequestDto;
    import com.harish.student_management.dto.CalendarEventResponseDto;
    import com.harish.student_management.service.CalendarEventService;
    import jakarta.validation.Valid;
    import lombok.RequiredArgsConstructor;
    import org.springframework.http.*;
    import org.springframework.security.access.prepost.PreAuthorize;
    import org.springframework.web.bind.annotation.*;

    @RestController
    @RequestMapping("/api/v1/teacher-events")
    @RequiredArgsConstructor
    public class TeacherCalendarEventController {
        private final CalendarEventService service;

        @PostMapping
        @PreAuthorize("hasRole('TEACHER')")
        public ResponseEntity<CalendarEventResponseDto> create(@Valid @RequestBody CalendarEventRequestDto r) {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r));
        }
    }
