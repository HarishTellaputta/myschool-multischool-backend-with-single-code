package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.Attendance;
import com.harish.student_management.entity.ClassEntity;
import com.harish.student_management.entity.Student;
import com.harish.student_management.entity.Teacher;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.AttendanceRepository;
import com.harish.student_management.repository.ClassRepository;
import com.harish.student_management.repository.StudentRepository;
import com.harish.student_management.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceServiceImpl.class);

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherAccessService teacherAccessService;

    @Override
    public AttendanceResponseDto markAttendance(AttendanceRequestDto request) {
        logger.info("Marking attendance for student: {} on date: {}", request.getStudentId(), request.getAttendanceDate());

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.getStudentId()));

        ClassEntity classEntity = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + request.getClassId()));

        validateStudentBelongsToClass(student, classEntity);

        Teacher teacher = null;

        if (!isSchoolAdmin()) {
            if (request.getTeacherId() == null) {
                throw new IllegalArgumentException("Teacher ID is required for teacher attendance marking");
            }

            teacher = teacherRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Teacher not found with id: " + request.getTeacherId()
                    ));

            teacherAccessService.validateTeacherAccessToClass(
                    request.getTeacherId(),
                    request.getClassId()
            );
        }
        if (!isValidStatus(request.getStatus())) {
            throw new IllegalArgumentException("Invalid status. Must be PRESENT, ABSENT, or LEAVE");
        }

        if (attendanceRepository.existsByStudentIdAndClassIdAndAttendanceDate(
                request.getStudentId(), request.getClassId(), request.getAttendanceDate())) {
            throw new IllegalArgumentException("Attendance already marked for this student on this date. Use update endpoint to modify.");
        }

        Attendance attendance = Attendance.builder()
                .student(student)
                .classEntity(classEntity)
                .markedByTeacher(teacher)
                .attendanceDate(request.getAttendanceDate())
                .status(request.getStatus())
                .remarks(request.getRemarks())
                .build();

        Attendance savedAttendance = attendanceRepository.save(attendance);
        logger.info("Attendance marked successfully with ID: {}", savedAttendance.getId());

        return mapToResponseDto(savedAttendance);
    }

    @Override
    @Transactional
    public List<AttendanceResponseDto> markBulkAttendance(
            BulkAttendanceRequestDto request) {

        logger.info(
                "Marking bulk attendance for class: {} on date: {}",
                request.getClassId(),
                request.getAttendanceDate()
        );

        ClassEntity classEntity = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found with id: " + request.getClassId()
                ));

        final Teacher teacher;

        // ============================================================
        // TEACHER
        // ============================================================

        if (!isSchoolAdmin()) {

            if (request.getTeacherId() == null) {
                throw new IllegalArgumentException(
                        "Teacher ID is required for teacher attendance marking"
                );
            }

            teacher = teacherRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Teacher not found with id: "
                                    + request.getTeacherId()
                    ));

            teacherAccessService.validateTeacherAccessToClass(
                    request.getTeacherId(),
                    request.getClassId()
            );

        }

        // ============================================================
        // SCHOOL ADMIN
        // ============================================================

        else {
            teacher = null;
        }

        // ============================================================
        // CREATE ATTENDANCE
        // ============================================================

        return request.getAttendanceRecords().stream()
                .map(record -> {

                    Student student = studentRepository
                            .findById(record.getStudentId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Student not found with id: "
                                            + record.getStudentId()
                            ));

                    validateStudentBelongsToClass(
                            student,
                            classEntity
                    );

                    if (!isValidStatus(record.getStatus())) {
                        throw new IllegalArgumentException(
                                "Invalid status. Must be PRESENT, ABSENT, or LEAVE"
                        );
                    }

                    if (attendanceRepository
                            .existsByStudentIdAndClassIdAndAttendanceDate(
                                    record.getStudentId(),
                                    request.getClassId(),
                                    request.getAttendanceDate()
                            )) {

                        throw new IllegalArgumentException(
                                "Attendance already marked for student "
                                        + record.getStudentId()
                                        + " on this date"
                        );
                    }

                    Attendance attendance = Attendance.builder()
                            .student(student)
                            .classEntity(classEntity)
                            .markedByTeacher(teacher)
                            .attendanceDate(request.getAttendanceDate())
                            .status(record.getStatus())
                            .remarks(record.getRemarks())
                            .build();

                    return attendanceRepository.save(attendance);
                })
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    @Override
    public AttendanceResponseDto getAttendanceById(Long id) {
        logger.info("Fetching attendance with ID: {}", id);
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found with id: " + id));
        return mapToResponseDto(attendance);
    }

    @Override
    public AttendanceResponseDto updateAttendance(Long id, AttendanceUpdateDto request) {
        logger.info("Updating attendance with ID: {}", id);
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found with id: " + id));

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + request.getTeacherId()));

        teacherAccessService.validateTeacherAccessToClass(request.getTeacherId(), attendance.getClassEntity().getId());

        if (!isValidStatus(request.getStatus())) {
            throw new IllegalArgumentException("Invalid status. Must be PRESENT, ABSENT, or LEAVE");
        }

        attendance.setStatus(request.getStatus());
        attendance.setRemarks(request.getRemarks());
        attendance.setUpdatedByTeacher(teacher);

        Attendance updatedAttendance = attendanceRepository.save(attendance);
        logger.info("Attendance updated successfully with ID: {}", updatedAttendance.getId());

        return mapToResponseDto(updatedAttendance);
    }

    @Override
    public void deleteAttendance(Long id) {
        logger.info("Deleting attendance with ID: {}", id);
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found with id: " + id));
        attendanceRepository.delete(attendance);
        logger.info("Attendance deleted successfully");
    }

    @Override
    public List<AttendanceResponseDto> getStudentAttendance(Long studentId, LocalDate startDate, LocalDate endDate) {
        logger.info("Fetching attendance for student: {} between {} and {}", studentId, startDate, endDate);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        return attendanceRepository.findByStudentIdAndAttendanceDateBetween(studentId, startDate, endDate).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceResponseDto> getClassAttendance(Long classId, LocalDate date) {
        logger.info("Fetching attendance for class: {} on date: {}", classId, date);
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        return attendanceRepository.findByClassIdAndAttendanceDate(classId, date).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceResponseDto> getClassAttendanceByDateRange(Long classId, LocalDate startDate, LocalDate endDate) {
        logger.info("Fetching attendance for class: {} between {} and {}", classId, startDate, endDate);
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        return attendanceRepository.findByClassIdAndAttendanceDateBetween(classId, startDate, endDate).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public AttendanceReportDto getStudentAttendanceReport(Long studentId, Long classId, LocalDate startDate, LocalDate endDate) {
        logger.info("Generating attendance report for student: {} in class: {} between {} and {}", studentId, classId, startDate, endDate);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        validateStudentBelongsToClass(student, classEntity);

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        long totalDays = attendanceRepository.countByStudentIdAndClassIdAndDateRange(studentId, classId, startDate, endDate);
        long presentDays = attendanceRepository.countByStudentIdAndClassIdAndDateRangeAndStatus(studentId, classId, startDate, endDate, "PRESENT");
        long absentDays = attendanceRepository.countByStudentIdAndClassIdAndDateRangeAndStatus(studentId, classId, startDate, endDate, "ABSENT");
        long leaveDays = attendanceRepository.countByStudentIdAndClassIdAndDateRangeAndStatus(studentId, classId, startDate, endDate, "LEAVE");

        double percentage = totalDays > 0 ? (presentDays * 100.0) / totalDays : 0.0;

        return AttendanceReportDto.builder()
                .studentId(studentId)
                .studentName(student.getName())
                .studentRollNumber(student.getRollNumber())
                .classId(classId)
                .className(classEntity.getName())
                .startDate(startDate)
                .endDate(endDate)
                .totalDays((int) totalDays)
                .presentDays((int) presentDays)
                .absentDays((int) absentDays)
                .leaveDays((int) leaveDays)
                .attendancePercentage(Math.round(percentage * 100.0) / 100.0)
                .build();
    }

    @Override
    public List<AttendanceReportDto> getClassAttendanceReport(Long classId, LocalDate startDate, LocalDate endDate) {
        logger.info("Generating attendance report for class: {} between {} and {}", classId, startDate, endDate);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        List<Student> students = studentRepository.findBySection_ClassEntity_Id(classId);

        return students.stream()
                .map(student -> getStudentAttendanceReport(student.getId(), classId, startDate, endDate))
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceResponseDto> getParentAttendance(Long parentId, LocalDate startDate, LocalDate endDate) {
        logger.info("Fetching attendance for parent's children: {} between {} and {}", parentId, startDate, endDate);

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        return attendanceRepository.findByParentIdAndDateRange(parentId, startDate, endDate).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public double calculateAttendancePercentage(Long studentId, Long classId, LocalDate startDate, LocalDate endDate) {
        logger.info("Calculating attendance percentage for student: {} in class: {}", studentId, classId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        long totalDays = attendanceRepository.countByStudentIdAndClassIdAndDateRange(studentId, classId, startDate, endDate);
        long presentDays = attendanceRepository.countByStudentIdAndClassIdAndDateRangeAndStatus(studentId, classId, startDate, endDate, "PRESENT");

        double percentage = totalDays > 0 ? (presentDays * 100.0) / totalDays : 0.0;
        return Math.round(percentage * 100.0) / 100.0;
    }

    private boolean isValidStatus(String status) {
        return status != null && (status.equals("PRESENT") || status.equals("ABSENT") || status.equals("LEAVE"));
    }

    private void validateStudentBelongsToClass(Student student, ClassEntity classEntity) {
        if (student.getSection() != null && student.getSection().getClassEntity() != null
                && !classEntity.getId().equals(student.getSection().getClassEntity().getId())) {
            throw new SecurityException("Student does not belong to this class");
        }
    }

    private AttendanceResponseDto mapToResponseDto(Attendance attendance) {
        return AttendanceResponseDto.builder()
                .id(attendance.getId())
                .studentId(attendance.getStudent() != null ? attendance.getStudent().getId() : null)
                .studentName(attendance.getStudent() != null ? attendance.getStudent().getName() : null)
                .studentRollNumber(attendance.getStudent() != null ? attendance.getStudent().getRollNumber() : null)
                .classId(attendance.getClassEntity() != null ? attendance.getClassEntity().getId() : null)
                .className(attendance.getClassEntity() != null ? attendance.getClassEntity().getName() : null)
                .markedByTeacherId(attendance.getMarkedByTeacher() != null ? attendance.getMarkedByTeacher().getId() : null)
                .markedByTeacherName(attendance.getMarkedByTeacher() != null ? attendance.getMarkedByTeacher().getName() : null)
                .attendanceDate(attendance.getAttendanceDate())
                .status(attendance.getStatus())
                .remarks(attendance.getRemarks())
                .markedAt(attendance.getMarkedAt())
                .updatedAt(attendance.getUpdatedAt())
                .updatedByTeacherId(attendance.getUpdatedByTeacher() != null ? attendance.getUpdatedByTeacher().getId() : null)
                .updatedByTeacherName(attendance.getUpdatedByTeacher() != null ? attendance.getUpdatedByTeacher().getName() : null)
                .build();
    }

    @Override
    public AttendanceDashboardSummaryDto getDashboardSummary() {

        LocalDate today = LocalDate.now();

        logger.info("Generating attendance dashboard summary for date: {}", today);

        long totalStudents = studentRepository.count();

        long present = attendanceRepository
                .countByAttendanceDateAndStatus(today, "PRESENT");

        long absent = attendanceRepository
                .countByAttendanceDateAndStatus(today, "ABSENT");

        long leave = attendanceRepository
                .countByAttendanceDateAndStatus(today, "LEAVE");

        long marked = attendanceRepository.countByAttendanceDate(today);

        double percentage = totalStudents > 0
                ? (present * 100.0) / totalStudents
                : 0.0;

        percentage = Math.round(percentage * 100.0) / 100.0;

        return AttendanceDashboardSummaryDto.builder()
                .totalStudents(totalStudents)
                .marked(marked)
                .present(present)
                .absent(absent)
                .leave(leave)
                .attendancePercentage(percentage)
                .build();
    }

    @Override
    public List<AttendanceLastSixDaysDto> getLastSixDaysAttendance() {

        LocalDate today = LocalDate.now();

        return java.util.stream.IntStream.rangeClosed(0, 5)
                .mapToObj(i -> {
                    LocalDate date = today.minusDays(5 - i);

                    long present =
                            attendanceRepository.countByAttendanceDateAndStatus(
                                    date, "PRESENT");

                    long absent =
                            attendanceRepository.countByAttendanceDateAndStatus(
                                    date, "ABSENT");

                    long leave =
                            attendanceRepository.countByAttendanceDateAndStatus(
                                    date, "LEAVE");

                    long totalStudents = studentRepository.count();

                    long marked = present + absent + leave;

                    double percentage = totalStudents > 0
                            ? (present * 100.0) / totalStudents
                            : 0.0;

                    percentage = Math.round(percentage * 100.0) / 100.0;

                    return AttendanceLastSixDaysDto.builder()
                            .date(date)
                            .day(date.getDayOfWeek().toString().substring(0, 3))
                            .attendancePercentage(percentage)
                            .present(present)
                            .absent(absent)
                            .leave(leave)
                            .totalStudents(totalStudents)
                            .marked(marked)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceReportDto> getAttendanceReport(
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {

        if (to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "End date must not precede start date"
            );
        }

        Page<Student> studentPage = studentRepository.findAll(pageable);

        return studentPage.map(student -> {

            Long classId = null;
            String className = null;

            if (student.getSection() != null &&
                    student.getSection().getClassEntity() != null) {

                classId = student.getSection().getClassEntity().getId();
                className = student.getSection().getClassEntity().getName();
            }

            long totalDays = 0;
            long presentDays = 0;
            long absentDays = 0;
            long leaveDays = 0;

            if (classId != null) {

                totalDays =
                        attendanceRepository.countByStudentIdAndClassIdAndDateRange(
                                student.getId(),
                                classId,
                                from,
                                to
                        );

                presentDays =
                        attendanceRepository.countByStudentIdAndClassIdAndDateRangeAndStatus(
                                student.getId(),
                                classId,
                                from,
                                to,
                                "PRESENT"
                        );

                absentDays =
                        attendanceRepository.countByStudentIdAndClassIdAndDateRangeAndStatus(
                                student.getId(),
                                classId,
                                from,
                                to,
                                "ABSENT"
                        );

                leaveDays =
                        attendanceRepository.countByStudentIdAndClassIdAndDateRangeAndStatus(
                                student.getId(),
                                classId,
                                from,
                                to,
                                "LEAVE"
                        );
            }

            double percentage = totalDays > 0
                    ? (presentDays * 100.0) / totalDays
                    : 0.0;

            return AttendanceReportDto.builder()
                    .studentId(student.getId())
                    .studentName(student.getName())
                    .studentRollNumber(student.getRollNumber())
                    .classId(classId)
                    .className(className)
                    .startDate(from)
                    .endDate(to)
                    .totalDays((int) totalDays)
                    .presentDays((int) presentDays)
                    .absentDays((int) absentDays)
                    .leaveDays((int) leaveDays)
                    .attendancePercentage(
                            Math.round(percentage * 100.0) / 100.0
                    )
                    .build();
        });
    }


    @Override
    @Transactional
    public List<AttendanceResponseDto> updateBulkAttendance(
            BulkAttendanceUpdateRequestDto request) {

        logger.info(
                "Updating bulk attendance records: {}",
                request.getAttendanceRecords().size()
        );

        final Teacher teacher;

        if (!isSchoolAdmin()) {
            if (request.getTeacherId() == null) {
                throw new IllegalArgumentException(
                        "Teacher ID is required for teacher attendance update"
                );
            }

            teacher = teacherRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Teacher not found with id: " + request.getTeacherId()
                    ));
        } else {
            teacher = null;
        }

        return request.getAttendanceRecords().stream()
                .map(record -> {

                    Attendance attendance = attendanceRepository
                            .findById(record.getAttendanceId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Attendance not found with id: "
                                            + record.getAttendanceId()
                            ));

                    if (!isSchoolAdmin()) {
                        teacherAccessService.validateTeacherAccessToClass(
                                request.getTeacherId(),
                                attendance.getClassEntity().getId()
                        );
                    }

                    if (!isValidStatus(record.getStatus())) {
                        throw new IllegalArgumentException(
                                "Invalid status. Must be PRESENT, ABSENT, or LEAVE"
                        );
                    }

                    attendance.setStatus(record.getStatus());
                    attendance.setRemarks(record.getRemarks());

                    if (teacher != null) {
                        attendance.setUpdatedByTeacher(teacher);
                    }

                    return attendanceRepository.save(attendance);
                })
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    private boolean isSchoolAdmin() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_SCHOOL_ADMIN"));
    }
    }
