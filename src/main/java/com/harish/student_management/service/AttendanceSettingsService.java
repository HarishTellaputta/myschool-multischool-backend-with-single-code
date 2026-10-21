
package com.harish.student_management.service;

import com.harish.student_management.dto.AttendanceSettingsDto;
import com.harish.student_management.entity.School;
import com.harish.student_management.repository.SchoolRepository;
import com.harish.student_management.service.CurrentSchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AttendanceSettingsService {

    private final CurrentSchoolService currentSchoolService;
    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public AttendanceSettingsDto getSettings() {
        School school = getCurrentSchool();

        String mode = normalizeMode(school.getAttendanceMode());
        Integer periodCount = school.getAttendancePeriodCount();

        if (periodCount == null || periodCount < 1 || periodCount > 20) {
            periodCount = 8;
        }

        return AttendanceSettingsDto.builder()
                .attendanceMode(mode)
                .attendancePeriodCount(periodCount)
                .build();
    }

    @Transactional
    public AttendanceSettingsDto updateSettings(AttendanceSettingsDto request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Attendance settings request is required"
            );
        }

        School school = getCurrentSchool();

        String mode = normalizeMode(request.getAttendanceMode());
        Integer periodCount = request.getAttendancePeriodCount();

        if (periodCount == null || periodCount < 1 || periodCount > 20) {
            throw new IllegalArgumentException(
                    "Attendance period count must be between 1 and 20"
            );
        }

        school.setAttendanceMode(mode);
        school.setAttendancePeriodCount(periodCount);

        School savedSchool = schoolRepository.save(school);

        return AttendanceSettingsDto.builder()
                .attendanceMode(normalizeMode(savedSchool.getAttendanceMode()))
                .attendancePeriodCount(savedSchool.getAttendancePeriodCount())
                .build();
    }

    private School getCurrentSchool() {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return schoolRepository.findById(schoolId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "School assigned to the current user was not found"
                        )
                );
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "SINGLE";
        }

        String normalized = mode.trim().toUpperCase(Locale.ROOT);

        if (!normalized.equals("SINGLE")
                && !normalized.equals("PERIOD_WISE")) {
            throw new IllegalArgumentException(
                    "Attendance mode must be SINGLE or PERIOD_WISE"
            );
        }

        return normalized;
    }
}
