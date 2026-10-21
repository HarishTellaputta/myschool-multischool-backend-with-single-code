
package com.harish.student_management.service;

import com.harish.student_management.entity.Teacher;
import com.harish.student_management.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmployeeIdGeneratorService {

    private static final String PREFIX = "EMP";

    private static final Pattern EMPLOYEE_ID_PATTERN =
            Pattern.compile("^EMP(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final TeacherRepository teacherRepository;

    /**
     * Generates a globally unique employee ID.
     * The schoolId is validated to ensure the caller supplies a school.
     * IDs remain globally unique because the current entity uses a
     * globally unique employeeId constraint.
     */
    public String generateNextEmployeeId(Long schoolId) {

        if (schoolId == null) {
            throw new IllegalArgumentException(
                    "School ID is required to generate an employee ID"
            );
        }

        int maxNumber = 0;

        for (Teacher teacher : teacherRepository.findAllByOrderByIdAsc()) {

            String employeeId = teacher.getEmployeeId();

            if (employeeId == null || employeeId.isBlank()) {
                continue;
            }

            Matcher matcher =
                    EMPLOYEE_ID_PATTERN.matcher(employeeId.trim());

            if (matcher.matches()) {
                try {
                    int number = Integer.parseInt(matcher.group(1));
                    maxNumber = Math.max(maxNumber, number);
                } catch (NumberFormatException ignored) {
                    // Ignore IDs with numeric suffixes too large for an int.
                }
            }
        }

        int nextNumber = maxNumber + 1;
        String employeeId;

        do {
            employeeId = PREFIX + String.format("%03d", nextNumber);
            nextNumber++;
        } while (teacherRepository.existsByEmployeeId(employeeId));

        return employeeId;
    }
}
