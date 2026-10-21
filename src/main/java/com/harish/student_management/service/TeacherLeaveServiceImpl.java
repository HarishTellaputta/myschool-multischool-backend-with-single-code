
package com.harish.student_management.service;

import com.harish.student_management.dto.TeacherLeaveRequestDto;
import com.harish.student_management.dto.TeacherLeaveResponseDto;
import com.harish.student_management.entity.Teacher;
import com.harish.student_management.entity.TeacherLeave;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.TeacherLeaveRepository;
import com.harish.student_management.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherLeaveServiceImpl implements TeacherLeaveService {

    private static final Logger logger =
            LoggerFactory.getLogger(TeacherLeaveServiceImpl.class);

    private final TeacherLeaveRepository leaveRepository;
    private final TeacherRepository teacherRepository;
    private final CurrentSchoolService currentSchoolService;

    @Override
    public TeacherLeaveResponseDto applyForLeave(
            TeacherLeaveRequestDto request) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();
        Teacher teacher;

        if (isSchoolAdmin()) {
            teacher = teacherRepository
                    .findByIdAndSchoolId(request.getTeacherId(), schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Teacher not found in your school"));
        } else {
            // Teachers can apply only for themselves.
            teacher = getCurrentTeacher(schoolId);
        }

        validateDates(request.getStartDate(), request.getEndDate());

        checkOverlap(
                teacher.getId(),
                request.getStartDate(),
                request.getEndDate(),
                null
        );

        TeacherLeave leave = TeacherLeave.builder()
                .teacher(teacher)
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .status("APPLIED")
                .build();

        TeacherLeave saved = leaveRepository.save(leave);

        logger.info("Leave application created: {}", saved.getId());

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherLeaveResponseDto getLeaveById(Long id) {
        TeacherLeave leave = getSchoolLeave(id);
        verifyTeacherOwnsLeaveIfTeacher(leave);
        return mapToResponseDto(leave);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherLeaveResponseDto> getLeavesByTeacherId(
            Long teacherId) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();
        verifyTeacherAccessToTeacherId(teacherId, schoolId);

        return leaveRepository
                .findAllByTeacher_IdAndTeacher_School_IdOrderByStartDateDesc(
                        teacherId, schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherLeaveResponseDto> getLeavesByTeacherIdAndStatus(
            Long teacherId, String status) {

        return getLeavesByTeacherId(teacherId).stream()
                .filter(leave -> leave.getStatus() != null
                        && leave.getStatus().equalsIgnoreCase(status))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherLeaveResponseDto> getLeavesByStatus(String status) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return leaveRepository
                .findAllByTeacher_School_IdOrderByStartDateDesc(schoolId)
                .stream()
                .filter(leave -> leave.getStatus() != null
                        && leave.getStatus().equalsIgnoreCase(status))
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherLeaveResponseDto> getAllSchoolLeaves() {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return leaveRepository
                .findAllByTeacher_School_IdOrderByStartDateDesc(schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public TeacherLeaveResponseDto updateLeave(
            Long id, TeacherLeaveRequestDto request) {

        TeacherLeave leave = getSchoolLeave(id);
        verifyTeacherOwnsLeaveIfTeacher(leave);

        validateDates(request.getStartDate(), request.getEndDate());

        checkOverlap(
                leave.getTeacher().getId(),
                request.getStartDate(),
                request.getEndDate(),
                leave.getId()
        );

        // Do not allow changing the teacher associated with an existing leave.
        leave.setLeaveType(request.getLeaveType());
        leave.setStartDate(request.getStartDate());
        leave.setEndDate(request.getEndDate());
        leave.setReason(request.getReason());

        // Approval/rejection fields are not used by this workflow.
        TeacherLeave updated = leaveRepository.save(leave);

        logger.info("Leave application updated: {}", updated.getId());

        return mapToResponseDto(updated);
    }

    @Override
    public void deleteLeave(Long id) {
        TeacherLeave leave = getSchoolLeave(id);
        verifyTeacherOwnsLeaveIfTeacher(leave);

        leaveRepository.delete(leave);
        logger.info("Leave application deleted: {}", id);
    }

    private TeacherLeave getSchoolLeave(Long id) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return leaveRepository.findByIdAndTeacher_School_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Leave application not found in your school: " + id));
    }

    private Teacher getCurrentTeacher(Long schoolId) {
        String username = currentUsername();

        return teacherRepository
                .findByUsernameAndSchoolId(username, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher profile not found for the logged-in user"));
    }

    private void verifyTeacherOwnsLeaveIfTeacher(TeacherLeave leave) {
        if (isSchoolAdmin()) {
            return;
        }

        Teacher currentTeacher = getCurrentTeacher(
                currentSchoolService.getCurrentSchoolId());

        if (!currentTeacher.getId().equals(leave.getTeacher().getId())) {
            throw new AccessDeniedException(
                    "You can access only your own leave applications");
        }
    }

    private void verifyTeacherAccessToTeacherId(
            Long teacherId, Long schoolId) {

        if (isSchoolAdmin()) {
            boolean belongsToSchool =
                    teacherRepository.existsByIdAndSchool_Id(
                            teacherId, schoolId);

            if (!belongsToSchool) {
                throw new ResourceNotFoundException(
                        "Teacher not found in your school");
            }
            return;
        }

        Teacher currentTeacher = getCurrentTeacher(schoolId);

        if (!currentTeacher.getId().equals(teacherId)) {
            throw new AccessDeniedException(
                    "You can view only your own leave applications");
        }
    }

    private void checkOverlap(
            Long teacherId,
            LocalDate startDate,
            LocalDate endDate,
            Long excludeLeaveId) {

        boolean overlap;

        if (excludeLeaveId == null) {
            overlap = leaveRepository
                    .existsByTeacher_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            teacherId, endDate, startDate);
        } else {
            overlap = leaveRepository
                    .existsByTeacher_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                            teacherId, endDate, startDate, excludeLeaveId);
        }

        if (overlap) {
            throw new IllegalArgumentException(
                    "Teacher already has a leave application during this period");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date must be on or after start date");
        }
    }

    private boolean isSchoolAdmin() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_SCHOOL_ADMIN")
                                || authority.getAuthority().equals("SCHOOL_ADMIN"));
    }

    private String currentUsername() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication required");
        }

        return authentication.getName();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherLeaveResponseDto> getLeavesByYear(int year) {
        if (year < 1900 || year > 2200) {
            throw new IllegalArgumentException("Invalid year");
        }

        return getLeavesForPeriod(
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherLeaveResponseDto> getLeavesByYearAndMonth(
            int year, int month) {

        final YearMonth yearMonth;

        try {
            yearMonth = YearMonth.of(year, month);
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException(
                    "Invalid year or month. Month must be between 1 and 12.");
        }

        return getLeavesForPeriod(
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
        );
    }

    private List<TeacherLeaveResponseDto> getLeavesForPeriod(
            LocalDate periodStart,
            LocalDate periodEnd) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();
        List<TeacherLeave> leaves;

        if (isSchoolAdmin()) {
            leaves = leaveRepository
                    .findAllByTeacher_School_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                            schoolId, periodEnd, periodStart);
        } else {
            Teacher teacher = getCurrentTeacher(schoolId);

            leaves = leaveRepository
                    .findAllByTeacher_IdAndTeacher_School_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                            teacher.getId(), schoolId, periodEnd, periodStart);
        }

        return leaves.stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    private TeacherLeaveResponseDto mapToResponseDto(TeacherLeave leave) {
        return TeacherLeaveResponseDto.builder()
                .id(leave.getId())
                .teacherId(leave.getTeacher() != null
                        ? leave.getTeacher().getId() : null)
                .teacherName(leave.getTeacher() != null
                        ? leave.getTeacher().getName() : null)
                .leaveType(leave.getLeaveType())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .reason(leave.getReason())
                .status(leave.getStatus())
                .appliedAt(leave.getAppliedAt())
                .createdAt(leave.getCreatedAt())
                .updatedAt(leave.getUpdatedAt())
                .build();
    }
}
