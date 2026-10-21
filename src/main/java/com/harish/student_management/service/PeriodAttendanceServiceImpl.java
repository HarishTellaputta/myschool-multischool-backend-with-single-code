
package com.harish.student_management.service;

import com.harish.student_management.dto.PeriodAttendanceRequestDto;
import com.harish.student_management.dto.PeriodAttendanceResponseDto;
import com.harish.student_management.entity.AttendancePeriod;
import com.harish.student_management.entity.ClassEntity;
import com.harish.student_management.entity.School;
import com.harish.student_management.entity.Student;
import com.harish.student_management.entity.Teacher;
import com.harish.student_management.entity.User;
import com.harish.student_management.repository.AttendancePeriodRepository;
import com.harish.student_management.repository.ClassRepository;
import com.harish.student_management.repository.SchoolRepository;
import com.harish.student_management.repository.StudentRepository;
import com.harish.student_management.repository.TeacherRepository;
import com.harish.student_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class PeriodAttendanceServiceImpl implements PeriodAttendanceService {

    private final AttendancePeriodRepository attendancePeriodRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final CurrentSchoolService currentSchoolService;
    private final TeacherAccessService teacherAccessService;

    @Override
    public List<PeriodAttendanceResponseDto> markPeriodAttendance(
            PeriodAttendanceRequestDto request) {

        if (request == null) {
            throw new IllegalArgumentException("Attendance request is required");
        }

        Long schoolId = currentSchoolService.getCurrentSchoolId();
        School school = getSchool(schoolId);

        validatePeriodMode(school);

        Integer periodNumber = request.getPeriodNumber();
        Integer configuredCount = school.getAttendancePeriodCount();

        if (periodNumber == null || periodNumber < 1
                || periodNumber > configuredCount) {
            throw new IllegalArgumentException(
                    "Period number must be between 1 and " + configuredCount);
        }

        LocalDate attendanceDate = request.getAttendanceDate();
        if (attendanceDate == null) {
            throw new IllegalArgumentException("Attendance date is required");
        }

        ClassEntity classEntity = classRepository
                .findByIdAndSchool_Id(request.getClassId(), schoolId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Class not found in the current school"));

        Teacher teacher = getCurrentTeacher(schoolId);
        validateTeacherAccess(teacher, classEntity);

        List<PeriodAttendanceResponseDto> responses = new ArrayList<>();

        for (PeriodAttendanceRequestDto.StudentPeriodAttendanceDto item
                : request.getStudents()) {

            if (item == null || item.getStudentId() == null) {
                throw new IllegalArgumentException("Student ID is required");
            }

            String status = normalizeStatus(item.getStatus());

            Student student = studentRepository
                    .findByIdAndSection_ClassEntity_School_Id(
                            item.getStudentId(), schoolId)
                    .orElseThrow(() ->
                            new AccessDeniedException(
                                    "Student not found in the current school"));

            if (student.getSection() == null
                    || student.getSection().getClassEntity() == null
                    || !classEntity.getId().equals(
                    student.getSection().getClassEntity().getId())) {
                throw new IllegalArgumentException(
                        "Student does not belong to the selected class");
            }

            boolean duplicate =
                    attendancePeriodRepository
                            .existsByStudent_IdAndClassEntity_IdAndAttendanceDateAndPeriodNumber(
                                    student.getId(),
                                    classEntity.getId(),
                                    attendanceDate,
                                    periodNumber);

            if (duplicate) {
                throw new IllegalArgumentException(
                        "Attendance already exists for student "
                                + student.getId()
                                + " in period " + periodNumber);
            }

            AttendancePeriod attendance = AttendancePeriod.builder()
                    .student(student)
                    .classEntity(classEntity)
                    .markedByTeacher(teacher)
                    .attendanceDate(attendanceDate)
                    .periodNumber(periodNumber)
                    .status(status)
                    .remarks(item.getRemarks())
                    .markedAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            responses.add(toDto(attendancePeriodRepository.save(attendance)));
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeriodAttendanceResponseDto> getClassPeriodAttendance(
            Long classId,
            LocalDate attendanceDate,
            Integer periodNumber) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        ClassEntity classEntity = classRepository
                .findByIdAndSchool_Id(classId, schoolId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Class not found in the current school"));

        if (attendanceDate == null) {
            throw new IllegalArgumentException("Attendance date is required");
        }

        if (periodNumber == null || periodNumber < 1) {
            throw new IllegalArgumentException("Valid period number is required");
        }

        School school = getSchool(schoolId);
        validatePeriodMode(school);

        if (periodNumber > school.getAttendancePeriodCount()) {
            throw new IllegalArgumentException("Period number exceeds school settings");
        }

        validateReadClassAccess(classEntity, schoolId);

        return attendancePeriodRepository
                .findByClassEntity_IdAndClassEntity_School_IdAndAttendanceDateBetweenOrderByAttendanceDateAscPeriodNumberAsc(
                        classId, schoolId, attendanceDate, attendanceDate)
                .stream()
                .filter(a -> a.getPeriodNumber().equals(periodNumber))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeriodAttendanceResponseDto> getStudentPeriodAttendance(
            Long studentId,
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A valid date range is required");
        }

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        Student student = studentRepository
                .findByIdAndSection_ClassEntity_School_Id(studentId, schoolId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Student not found in the current school"));

        validateStudentReadAccess(student, schoolId);

        return attendancePeriodRepository
                .findByStudent_IdAndAttendanceDateBetweenOrderByAttendanceDateAscPeriodNumberAsc(
                        studentId, startDate, endDate)
                .stream()
                .filter(a -> a.getClassEntity().getSchool().getId().equals(schoolId))
                .map(this::toDto)
                .toList();
    }

    @Override
    public PeriodAttendanceResponseDto updatePeriodAttendance(
            Long attendancePeriodId,
            String status,
            String remarks) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        AttendancePeriod attendance = attendancePeriodRepository
                .findByIdAndClassEntity_School_Id(attendancePeriodId, schoolId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Attendance record not found in the current school"));

        Teacher teacher = getCurrentTeacher(schoolId);
        validateTeacherAccess(teacher, attendance.getClassEntity());

        attendance.setStatus(normalizeStatus(status));
        attendance.setRemarks(remarks);
        attendance.setUpdatedByTeacher(teacher);
        attendance.setUpdatedAt(LocalDateTime.now());

        return toDto(attendancePeriodRepository.save(attendance));
    }

    @Override
    public void deletePeriodAttendance(Long attendancePeriodId) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        AttendancePeriod attendance = attendancePeriodRepository
                .findByIdAndClassEntity_School_Id(attendancePeriodId, schoolId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Attendance record not found in the current school"));

        requireSchoolAdmin();
        attendancePeriodRepository.delete(attendance);
    }

    private School getSchool(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() ->
                        new IllegalStateException("Current school was not found"));
    }

    private void validatePeriodMode(School school) {
        String mode = school.getAttendanceMode();

        if (mode == null
                || !"PERIOD_WISE".equalsIgnoreCase(mode.trim())) {
            throw new IllegalStateException(
                    "Period-wise attendance is not enabled for this school");
        }

        if (school.getAttendancePeriodCount() == null
                || school.getAttendancePeriodCount() < 1
                || school.getAttendancePeriodCount() > 20) {
            throw new IllegalStateException(
                    "School attendance period settings are invalid");
        }
    }

    private Teacher getCurrentTeacher(Long schoolId) {
        String username = getAuthenticatedUsername();

        return teacherRepository
                .findByUser_UsernameAndSchool_Id(username, schoolId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "The logged-in user is not a teacher in this school"));
    }

    private void validateTeacherAccess(Teacher teacher, ClassEntity classEntity) {
        teacherAccessService.validateTeacherAccessToClass(
                teacher.getId(), classEntity.getId());
    }

    private void validateReadClassAccess(
            ClassEntity classEntity,
            Long schoolId) {

        if (isSchoolAdmin()) {
            return;
        }

        Teacher teacher = getCurrentTeacher(schoolId);
        validateTeacherAccess(teacher, classEntity);
    }

    private void validateStudentReadAccess(Student student, Long schoolId) {
        if (isSchoolAdmin()) {
            return;
        }

        if (hasRole("TEACHER")) {
            Teacher teacher = getCurrentTeacher(schoolId);
            validateTeacherAccess(teacher, student.getSection().getClassEntity());
            return;
        }

        if (hasRole("PARENT")) {
            String username = getAuthenticatedUsername();

            User user = userRepository
                    .findByUsernameAndSchool_Id(username, schoolId)
                    .orElseThrow(() ->
                            new AccessDeniedException("Current user not found"));

            boolean ownsStudent = student.getParent() != null
                    && student.getParent().getUser() != null
                    && student.getParent().getUser().getId().equals(user.getId());

            if (!ownsStudent) {
                throw new AccessDeniedException(
                        "You can only view attendance for your own children");
            }
            return;
        }

        if (hasRole("STUDENT")) {
            String username = getAuthenticatedUsername();

            boolean ownStudent = student.getEmail() != null
                    && student.getEmail().equalsIgnoreCase(username);

            if (!ownStudent) {
                throw new AccessDeniedException(
                        "You can only view your own attendance");
            }
            return;
        }

        throw new AccessDeniedException("You cannot access this attendance record");
    }

    private void requireSchoolAdmin() {
        if (!isSchoolAdmin()) {
            throw new AccessDeniedException(
                    "Only a school admin can delete attendance");
        }
    }

    private boolean isSchoolAdmin() {
        return hasRole("SCHOOL_ADMIN");
    }

    private boolean hasRole(String role) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private String getAuthenticatedUsername() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("Authentication is required");
        }

        return authentication.getName();
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Attendance status is required");
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);

        if (!List.of("PRESENT", "ABSENT", "LEAVE").contains(normalized)) {
            throw new IllegalArgumentException(
                    "Status must be PRESENT, ABSENT, or LEAVE");
        }

        return normalized;
    }

    private PeriodAttendanceResponseDto toDto(AttendancePeriod attendance) {
        Student student = attendance.getStudent();
        ClassEntity classEntity = attendance.getClassEntity();
        Teacher markedBy = attendance.getMarkedByTeacher();
        Teacher updatedBy = attendance.getUpdatedByTeacher();

        return PeriodAttendanceResponseDto.builder()
                .id(attendance.getId())
                .studentId(student.getId())
                .studentName(student.getName())
                .rollNumber(student.getRollNumber())
                .classId(classEntity.getId())
                .className(classEntity.getName())
                .attendanceDate(attendance.getAttendanceDate())
                .periodNumber(attendance.getPeriodNumber())
                .status(attendance.getStatus())
                .remarks(attendance.getRemarks())
                .markedByTeacherId(markedBy == null ? null : markedBy.getId())
                .markedByTeacherName(markedBy == null ? null : markedBy.getName())
                .updatedByTeacherId(updatedBy == null ? null : updatedBy.getId())
                .updatedByTeacherName(updatedBy == null ? null : updatedBy.getName())
                .markedAt(attendance.getMarkedAt())
                .updatedAt(attendance.getUpdatedAt())
                .build();
    }
}
