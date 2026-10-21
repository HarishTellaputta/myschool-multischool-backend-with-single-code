
package com.harish.student_management.controller;

import com.harish.student_management.dto.AttendanceSettingsDto;
import com.harish.student_management.service.AttendanceSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendance-settings")
@RequiredArgsConstructor
public class AttendanceSettingsController {

    private final AttendanceSettingsService attendanceSettingsService;

    @GetMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<AttendanceSettingsDto> getSettings() {
        return ResponseEntity.ok(
                attendanceSettingsService.getSettings()
        );
    }

    @PutMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<AttendanceSettingsDto> updateSettings(
            @Valid @RequestBody AttendanceSettingsDto request
    ) {
        return ResponseEntity.ok(
                attendanceSettingsService.updateSettings(request)
        );
    }
}
