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

    public String generateNextEmployeeId() {

        int maxNumber = 0;

        for (Teacher teacher : teacherRepository.findAll()) {

            String employeeId = teacher.getEmployeeId();

            if (employeeId == null || employeeId.isBlank()) {
                continue;
            }

            Matcher matcher = EMPLOYEE_ID_PATTERN.matcher(employeeId.trim());

            if (matcher.matches()) {

                try {
                    int number = Integer.parseInt(matcher.group(1));

                    if (number > maxNumber) {
                        maxNumber = number;
                    }

                } catch (NumberFormatException ignored) {
                    // Ignore invalid employee IDs
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