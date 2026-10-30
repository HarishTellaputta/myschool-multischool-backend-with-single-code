package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.*;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
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
    private final CurrentSchoolService currentSchoolService;
    private final UserRepository userRepository;
    private final ParentRepository  parentRepository;



    private Long getCurrentSchoolId() {
        return currentSchoolService.getCurrentSchoolId();
    }

    private String getCurrentUsername() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException(
                    "Authentication is required"
            );
        }

        return authentication.getName();
    }

    private boolean hasRole(String role) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_" + role));
    }

    private boolean isSchoolAdmin() {
        return hasRole("SCHOOL_ADMIN");
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException(
                    "Attendance status is required"
            );
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);

        if (!List.of("PRESENT", "ABSENT", "LEAVE")
                .contains(normalized)) {
            throw new IllegalArgumentException(
                    "Status must be PRESENT, ABSENT, or LEAVE"
            );
        }

        return normalized;
    }
    @Override
    public AttendanceResponseDto markAttendance(AttendanceRequestDto request) {

        logger.info(
                "Marking attendance for student: {} on date: {}",
                request.getStudentId(),
                request.getAttendanceDate()
        );

        if (request.getStudentId() == null
                || request.getClassId() == null
                || request.getAttendanceDate() == null) {
            throw new IllegalArgumentException(
                    "Student ID, Class ID, and attendance date are required"
            );
        }

        Long schoolId = getCurrentSchoolId();

        // 1. Validate student belongs to the logged-in school's tenant
        Student student = studentRepository
                .findByIdAndSection_ClassEntity_School_Id(
                        request.getStudentId(),
                        schoolId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found in this school with id: "
                                + request.getStudentId()
                ));

        // 2. Validate class belongs to the same school
        ClassEntity classEntity = classRepository
                .findByIdAndSchool_Id(
                        request.getClassId(),
                        schoolId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found in this school with id: "
                                + request.getClassId()
                ));

        // 3. Validate student belongs to the requested class
        validateStudentBelongsToClass(student, classEntity);

        // 4. Validate and normalize attendance status
        String status = normalizeStatus(request.getStatus());

        Teacher teacher = null;

        // 5. For teachers, resolve identity from the authenticated session.
        // Never trust teacherId supplied by the client.
        if (!isSchoolAdmin()) {

            String username = getCurrentUsername();

            teacher = teacherRepository
                    .findByUser_UsernameAndSchool_Id(
                            username,
                            schoolId
                    )
                    .orElseThrow(() -> new AccessDeniedException(
                            "Authenticated teacher profile was not found "
                                    + "in this school"
                    ));

            // If the client supplies teacherId, it must match the logged-in teacher.
            if (request.getTeacherId() != null
                    && !request.getTeacherId().equals(teacher.getId())) {
                throw new AccessDeniedException(
                        "You cannot mark attendance as another teacher"
                );
            }

            // Verify that this teacher is assigned to the requested class.
            teacherAccessService.validateTeacherAccessToClass(
                    teacher.getId(),
                    classEntity.getId()
            );
        }

        Integer periodNumber = resolvePeriodNumber(
                classEntity,
                request.getPeriodNumber()
        );


        boolean alreadyMarked = attendanceRepository
                .existsByStudent_IdAndClassEntity_IdAndClassEntity_School_IdAndAttendanceDateAndPeriodNumber(
                        student.getId(),
                        classEntity.getId(),
                        schoolId,
                        request.getAttendanceDate(),
                        periodNumber
                );

        if (alreadyMarked) {
            throw new IllegalArgumentException(
                    "Attendance already marked for this student, date, and period. "
                            + "Use update endpoint to modify."
            );
        }

        // 7. Save attendance only after all validations succeed
        Attendance attendance = Attendance.builder()
                .student(student)
                .classEntity(classEntity)
                .markedByTeacher(teacher)
                .attendanceDate(request.getAttendanceDate())
                .periodNumber(periodNumber)
                .status(status)
                .remarks(request.getRemarks())
                .build();

        Attendance savedAttendance = attendanceRepository.save(attendance);

        logger.info(
                "Attendance marked successfully with ID: {}",
                savedAttendance.getId()
        );

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

        if (request.getClassId() == null
                || request.getAttendanceDate() == null) {
            throw new IllegalArgumentException(
                    "Class ID and attendance date are required"
            );
        }

        if (request.getAttendanceRecords() == null
                || request.getAttendanceRecords().isEmpty()) {
            throw new IllegalArgumentException(
                    "Attendance records are required"
            );
        }

        Long schoolId = getCurrentSchoolId();

        // 1. Validate that the class belongs to the current school
        ClassEntity classEntity = classRepository
                .findByIdAndSchool_Id(
                        request.getClassId(),
                        schoolId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found in this school with id: "
                                + request.getClassId()
                ));

        // 2. Resolve teacher from authenticated user, not request data
        Teacher teacher = null;

        if (!isSchoolAdmin()) {

            String username = getCurrentUsername();

            teacher = teacherRepository
                    .findByUser_UsernameAndSchool_Id(
                            username,
                            schoolId
                    )
                    .orElseThrow(() -> new AccessDeniedException(
                            "Teacher profile not found in this school"
                    ));

            if (request.getTeacherId() != null
                    && !request.getTeacherId().equals(teacher.getId())) {
                throw new AccessDeniedException(
                        "You cannot mark attendance as another teacher"
                );
            }

            // Ensure the logged-in teacher is assigned to this class
            teacherAccessService.validateTeacherAccessToClass(
                    teacher.getId(),
                    classEntity.getId()
            );
        }

        Integer periodNumber = resolvePeriodNumber(
                classEntity,
                request.getPeriodNumber()
        );

        final Teacher markedByTeacher = teacher;

        // 3. Validate all records before saving any attendance
        List<Attendance> attendanceToSave = new ArrayList<>();
        Set<Long> processedStudentIds = new HashSet<>();

        for (var record : request.getAttendanceRecords()) {

            if (record.getStudentId() == null) {
                throw new IllegalArgumentException(
                        "Student ID is required for every attendance record"
                );
            }

            if (!processedStudentIds.add(record.getStudentId())) {
                throw new IllegalArgumentException(
                        "Duplicate student ID in request: "
                                + record.getStudentId()
                );
            }

            // Student must belong to the current school
            Student student = studentRepository
                    .findByIdAndSection_ClassEntity_School_Id(
                            record.getStudentId(),
                            schoolId
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Student not found in this school with id: "
                                    + record.getStudentId()
                    ));

            // Student must belong to the requested class
            validateStudentBelongsToClass(student, classEntity);

            // Normalize and validate status
            String status = normalizeStatus(record.getStatus());


            boolean alreadyMarked = attendanceRepository
                    .existsByStudent_IdAndClassEntity_IdAndClassEntity_School_IdAndAttendanceDateAndPeriodNumber(
                            student.getId(),
                            classEntity.getId(),
                            schoolId,
                            request.getAttendanceDate(),
                            periodNumber
                    );

            if (alreadyMarked) {
                throw new IllegalArgumentException(
                        "Attendance already marked for student "
                                + student.getId()
                                + " on this date and period "
                                + periodNumber
                                + ". Use update endpoint to modify."
                );
            }


            Attendance attendance = Attendance.builder()
                    .student(student)
                    .classEntity(classEntity)
                    .markedByTeacher(markedByTeacher)
                    .attendanceDate(request.getAttendanceDate())
                    .periodNumber(periodNumber)
                    .status(status)
                    .remarks(record.getRemarks())
                    .build();

            attendanceToSave.add(attendance);
        }

        // 4. Save only after all records pass validation
        List<Attendance> savedRecords =
                attendanceRepository.saveAll(attendanceToSave);

        logger.info(
                "Bulk attendance marked successfully. Class ID: {}, Records: {}",
                classEntity.getId(),
                savedRecords.size()
        );

        return savedRecords.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public AttendanceResponseDto getAttendanceById(Long id) {

        logger.info("Fetching attendance with ID: {}", id);

        Long schoolId = getCurrentSchoolId();

        Attendance attendance = attendanceRepository
                .findByIdAndClassEntity_School_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attendance not found with id: " + id
                ));

        // Parent can only view their own child's attendance
        if (hasRole("PARENT")) {
            String username = getCurrentUsername();

            Parent parent = parentRepository
                    .findByUserUsername(username)
                    .orElseThrow(() -> new AccessDeniedException(
                            "Parent profile not found"
                    ));

            Parent schoolParent = parentRepository
                    .findByIdAndUser_School_Id(parent.getId(), schoolId)
                    .orElseThrow(() -> new AccessDeniedException(
                            "Parent does not belong to this school"
                    ));

            if (attendance.getStudent().getParent() == null
                    || !schoolParent.getId().equals(
                    attendance.getStudent().getParent().getId())) {
                throw new AccessDeniedException(
                        "You can only view your child's attendance"
                );
            }
        }

        // Teacher can only view attendance for an assigned class
        if (hasRole("TEACHER")) {
            Teacher teacher = teacherRepository
                    .findByUser_UsernameAndSchool_Id(
                            getCurrentUsername(), schoolId
                    )
                    .orElseThrow(() -> new AccessDeniedException(
                            "Teacher profile not found"
                    ));

            teacherAccessService.validateTeacherAccessToClass(
                    teacher.getId(),
                    attendance.getClassEntity().getId()
            );
        }

        return mapToResponseDto(attendance);
    }
    @Override
    public AttendanceResponseDto updateAttendance(
            Long id,
            AttendanceUpdateDto request) {

        logger.info("Updating attendance with ID: {}", id);

        Long schoolId = getCurrentSchoolId();

        Attendance attendance = attendanceRepository
                .findByIdAndClassEntity_School_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attendance not found with id: " + id
                ));

        Teacher teacher = null;

        if (!isSchoolAdmin()) {

            teacher = teacherRepository
                    .findByUser_UsernameAndSchool_Id(
                            getCurrentUsername(), schoolId
                    )
                    .orElseThrow(() -> new AccessDeniedException(
                            "Teacher profile not found"
                    ));

            if (request.getTeacherId() != null
                    && !request.getTeacherId().equals(teacher.getId())) {
                throw new AccessDeniedException(
                        "You cannot update attendance as another teacher"
                );
            }

            teacherAccessService.validateTeacherAccessToClass(
                    teacher.getId(),
                    attendance.getClassEntity().getId()
            );
        }

        String status = normalizeStatus(request.getStatus());

        attendance.setStatus(status);
        attendance.setRemarks(request.getRemarks());

        // Record who updated the attendance.
        // School admin updates may have no teacher profile.
        attendance.setUpdatedByTeacher(teacher);

        Attendance updatedAttendance =
                attendanceRepository.save(attendance);

        logger.info(
                "Attendance updated successfully with ID: {}",
                updatedAttendance.getId()
        );

        return mapToResponseDto(updatedAttendance);
    }
    @Override
    public void deleteAttendance(Long id) {

        logger.info("Deleting attendance with ID: {}", id);

        if (!isSchoolAdmin()) {
            throw new AccessDeniedException(
                    "Only school administrators can delete attendance"
            );
        }

        Long schoolId = getCurrentSchoolId();

        Attendance attendance = attendanceRepository
                .findByIdAndClassEntity_School_Id(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attendance not found with id: " + id
                ));

        attendanceRepository.delete(attendance);

        logger.info("Attendance deleted successfully");
    }
    @Override
    public List<AttendanceResponseDto> getStudentAttendance(
            Long studentId,
            LocalDate startDate,
            LocalDate endDate) {

        logger.info(
                "Fetching attendance for student: {} between {} and {}",
                studentId, startDate, endDate
        );

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date must not be before start date"
            );
        }

        Long schoolId = getCurrentSchoolId();

        Student student = studentRepository
                .findByIdAndSection_ClassEntity_School_Id(
                        studentId, schoolId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found in this school with id: "
                                + studentId
                ));

        // Parent must only access their own child.
        if (hasRole("PARENT")) {

            Parent parent = parentRepository
                    .findByUserUsername(getCurrentUsername())
                    .orElseThrow(() -> new AccessDeniedException(
                            "Parent profile not found"
                    ));

            Parent schoolParent = parentRepository
                    .findByIdAndUser_School_Id(parent.getId(), schoolId)
                    .orElseThrow(() -> new AccessDeniedException(
                            "Parent does not belong to this school"
                    ));

            if (student.getParent() == null
                    || !schoolParent.getId().equals(
                    student.getParent().getId())) {
                throw new AccessDeniedException(
                        "You can only view your child's attendance"
                );
            }
        }

        // Teacher must be assigned to the student's class.
        if (hasRole("TEACHER")) {

            Teacher teacher = teacherRepository
                    .findByUser_UsernameAndSchool_Id(
                            getCurrentUsername(), schoolId
                    )
                    .orElseThrow(() -> new AccessDeniedException(
                            "Teacher profile not found"
                    ));

            if (student.getSection() == null
                    || student.getSection().getClassEntity() == null) {
                throw new ResourceNotFoundException(
                        "Student class information not found"
                );
            }

            teacherAccessService.validateTeacherAccessToClass(
                    teacher.getId(),
                    student.getSection().getClassEntity().getId()
            );
        }

        // Student login ownership cannot yet be verified because
        // Student.java has no User mapping.
        if (hasRole("STUDENT")) {
            throw new AccessDeniedException(
                    "Student account ownership mapping must be configured "
                            + "before attendance access is enabled"
            );
        }

        return attendanceRepository
                .findByStudent_IdAndClassEntity_School_IdAndAttendanceDateBetween(
                        student.getId(),
                        schoolId,
                        startDate,
                        endDate
                )
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    @Override
    public List<AttendanceResponseDto> getClassAttendance(
            Long classId,
            LocalDate date) {

        logger.info(
                "Fetching attendance for class: {} on date: {}",
                classId, date
        );

        if (classId == null || date == null) {
            throw new IllegalArgumentException(
                    "Class ID and date are required"
            );
        }

        Long schoolId = getCurrentSchoolId();

        ClassEntity classEntity = classRepository
                .findByIdAndSchool_Id(classId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found in this school with id: " + classId
                ));

        // Teachers can access only their assigned classes.
        if (hasRole("TEACHER")) {

            Teacher teacher = teacherRepository
                    .findByUser_UsernameAndSchool_Id(
                            getCurrentUsername(), schoolId
                    )
                    .orElseThrow(() -> new AccessDeniedException(
                            "Teacher profile not found in this school"
                    ));

            teacherAccessService.validateTeacherAccessToClass(
                    teacher.getId(),
                    classEntity.getId()
            );
        }

        return attendanceRepository
                .findByClassEntity_IdAndClassEntity_School_IdAndAttendanceDate(
                        classEntity.getId(),
                        schoolId,
                        date
                )
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    @Override
    public List<AttendanceResponseDto> getClassAttendanceByDateRange(
            Long classId,
            LocalDate startDate,
            LocalDate endDate) {

        logger.info(
                "Fetching attendance for class: {} between {} and {}",
                classId, startDate, endDate
        );

        if (classId == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Class ID, start date, and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date must not be before start date"
            );
        }

        Long schoolId = getCurrentSchoolId();

        ClassEntity classEntity = classRepository
                .findByIdAndSchool_Id(classId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found in this school with id: " + classId
                ));

        if (hasRole("TEACHER")) {

            Teacher teacher = teacherRepository
                    .findByUser_UsernameAndSchool_Id(
                            getCurrentUsername(), schoolId
                    )
                    .orElseThrow(() -> new AccessDeniedException(
                            "Teacher profile not found in this school"
                    ));

            teacherAccessService.validateTeacherAccessToClass(
                    teacher.getId(),
                    classEntity.getId()
            );
        }

        return attendanceRepository
                .findByClassEntity_IdAndClassEntity_School_IdAndAttendanceDateBetween(
                        classEntity.getId(),
                        schoolId,
                        startDate,
                        endDate
                )
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    @Override
    public AttendanceReportDto getStudentAttendanceReport(
            Long studentId,
            Long classId,
            LocalDate startDate,
            LocalDate endDate) {

        logger.info(
                "Generating attendance report for student: {} in class: {} between {} and {}",
                studentId, classId, startDate, endDate
        );

        if (studentId == null || classId == null
                || startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Student ID, class ID, start date, and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date must not be before start date"
            );
        }

        Long schoolId = getCurrentSchoolId();

        // Ensure student belongs to the current school.
        Student student = studentRepository
                .findByIdAndSection_ClassEntity_School_Id(
                        studentId, schoolId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found in this school with id: " + studentId
                ));

        // Ensure class belongs to the current school.
        ClassEntity classEntity = classRepository
                .findByIdAndSchool_Id(classId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found in this school with id: " + classId
                ));

        validateStudentBelongsToClass(student, classEntity);

        // Parent can view only their own child's report.
        if (hasRole("PARENT")) {

            Parent parent = parentRepository
                    .findByUserUsername(getCurrentUsername())
                    .orElseThrow(() -> new AccessDeniedException(
                            "Parent profile not found"
                    ));

            Parent schoolParent = parentRepository
                    .findByIdAndUser_School_Id(parent.getId(), schoolId)
                    .orElseThrow(() -> new AccessDeniedException(
                            "Parent does not belong to this school"
                    ));

            if (student.getParent() == null
                    || !schoolParent.getId().equals(
                    student.getParent().getId())) {
                throw new AccessDeniedException(
                        "You can only view your child's attendance report"
                );
            }
        }

        // Teacher can view reports only for assigned classes.
        if (hasRole("TEACHER")) {

            Teacher teacher = teacherRepository
                    .findByUser_UsernameAndSchool_Id(
                            getCurrentUsername(), schoolId
                    )
                    .orElseThrow(() -> new AccessDeniedException(
                            "Teacher profile not found"
                    ));

            teacherAccessService.validateTeacherAccessToClass(
                    teacher.getId(),
                    classEntity.getId()
            );
        }

        // Student-to-user ownership mapping is not present in Student.java.
        if (hasRole("STUDENT")) {
            throw new AccessDeniedException(
                    "Student login ownership mapping must be configured "
                            + "before report access is enabled"
            );
        }

        long totalDays = attendanceRepository
                .countByStudentClassSchoolAndDateRange(
                        studentId, classId, schoolId, startDate, endDate
                );

        long presentDays = attendanceRepository
                .countByStudentClassSchoolDateRangeAndStatus(
                        studentId, classId, schoolId,
                        startDate, endDate, "PRESENT"
                );

        long absentDays = attendanceRepository
                .countByStudentClassSchoolDateRangeAndStatus(
                        studentId, classId, schoolId,
                        startDate, endDate, "ABSENT"
                );

        long leaveDays = attendanceRepository
                .countByStudentClassSchoolDateRangeAndStatus(
                        studentId, classId, schoolId,
                        startDate, endDate, "LEAVE"
                );

        double percentage = totalDays > 0
                ? (presentDays * 100.0) / totalDays
                : 0.0;

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
                .attendancePercentage(
                        Math.round(percentage * 100.0) / 100.0
                )
                .build();
    }
    @Override
    public List<AttendanceReportDto> getClassAttendanceReport(
            Long classId,
            LocalDate startDate,
            LocalDate endDate) {

        logger.info(
                "Generating attendance report for class: {} between {} and {}",
                classId, startDate, endDate
        );

        if (classId == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Class ID, start date, and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date must not be before start date"
            );
        }

        Long schoolId = getCurrentSchoolId();

        ClassEntity classEntity = classRepository
                .findByIdAndSchool_Id(classId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found in this school with id: " + classId
                ));

        // Teacher can access reports only for assigned classes.
        if (hasRole("TEACHER")) {

            Teacher teacher = teacherRepository
                    .findByUser_UsernameAndSchool_Id(
                            getCurrentUsername(), schoolId
                    )
                    .orElseThrow(() -> new AccessDeniedException(
                            "Teacher profile not found in this school"
                    ));

            teacherAccessService.validateTeacherAccessToClass(
                    teacher.getId(),
                    classEntity.getId()
            );
        }

        // Fetch only students belonging to this school's class.
        List<Student> students =
                studentRepository.findBySection_ClassEntity_IdAndSection_ClassEntity_School_Id(
                        classId, schoolId
                );

        return students.stream()
                .map(student -> getStudentAttendanceReport(
                        student.getId(),
                        classId,
                        startDate,
                        endDate
                ))
                .collect(Collectors.toList());
    }
    @Override
    public List<AttendanceResponseDto> getParentAttendance(
            Long parentId,
            LocalDate startDate,
            LocalDate endDate) {

        logger.info(
                "Fetching attendance for parent's children: {} between {} and {}",
                parentId, startDate, endDate
        );

        if (parentId == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Parent ID, start date, and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date must not be before start date"
            );
        }

        Long schoolId = getCurrentSchoolId();

        // Validate parent belongs to the current school.
        Parent requestedParent = parentRepository
                .findByIdAndUser_School_Id(parentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parent not found in this school with id: " + parentId
                ));

        // A parent must not access another parent's records.
        if (hasRole("PARENT")) {

            Parent loggedInParent = parentRepository
                    .findByUserUsername(getCurrentUsername())
                    .orElseThrow(() -> new AccessDeniedException(
                            "Parent profile not found"
                    ));

            if (!requestedParent.getId().equals(loggedInParent.getId())) {
                throw new AccessDeniedException(
                        "You can only view attendance for your own children"
                );
            }
        } else if (!isSchoolAdmin()) {
            throw new AccessDeniedException(
                    "You are not authorized to view parent attendance"
            );
        }

        return attendanceRepository
                .findParentAttendanceByParentAndSchoolAndDateRange(
                        requestedParent.getId(),
                        schoolId,
                        startDate,
                        endDate
                )
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }


    @Override
    public double calculateAttendancePercentage(
            Long studentId,
            Long classId,
            LocalDate startDate,
            LocalDate endDate) {

        logger.info(
                "Calculating attendance percentage for student: {} in class: {}",
                studentId, classId
        );

        if (studentId == null || classId == null
                || startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Student ID, class ID, start date, and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date must not be before start date"
            );
        }

        Long schoolId = getCurrentSchoolId();

        Student student = studentRepository
                .findByIdAndSection_ClassEntity_School_Id(
                        studentId, schoolId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found in this school with id: " + studentId
                ));

        ClassEntity classEntity = classRepository
                .findByIdAndSchool_Id(classId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found in this school with id: " + classId
                ));

        validateStudentBelongsToClass(student, classEntity);

        // Parent can calculate attendance only for their own child.
        if (hasRole("PARENT")) {

            Parent parent = parentRepository
                    .findByUserUsername(getCurrentUsername())
                    .orElseThrow(() -> new AccessDeniedException(
                            "Parent profile not found"
                    ));

            if (student.getParent() == null
                    || !parent.getId().equals(student.getParent().getId())) {
                throw new AccessDeniedException(
                        "You can only view your child's attendance percentage"
                );
            }
        }

        if (hasRole("TEACHER")) {

            Teacher teacher = teacherRepository
                    .findByUser_UsernameAndSchool_Id(
                            getCurrentUsername(), schoolId
                    )
                    .orElseThrow(() -> new AccessDeniedException(
                            "Teacher profile not found"
                    ));

            teacherAccessService.validateTeacherAccessToClass(
                    teacher.getId(),
                    classEntity.getId()
            );
        }

        // Student login ownership cannot be verified with the current
        // Student entity because it has no User relationship.
        if (hasRole("STUDENT")) {
            throw new AccessDeniedException(
                    "Student login ownership mapping must be configured "
                            + "before percentage access is enabled"
            );
        }

        long totalDays = attendanceRepository
                .countByStudentClassSchoolAndDateRange(
                        studentId, classId, schoolId, startDate, endDate
                );

        long presentDays = attendanceRepository
                .countByStudentClassSchoolDateRangeAndStatus(
                        studentId, classId, schoolId,
                        startDate, endDate, "PRESENT"
                );

        double percentage = totalDays > 0
                ? (presentDays * 100.0) / totalDays
                : 0.0;

        return Math.round(percentage * 100.0) / 100.0;
    }


    private boolean isValidStatus(String status) {
        return status != null && (status.equals("PRESENT") || status.equals("ABSENT") || status.equals("LEAVE"));
    }
    private void validateStudentBelongsToClass(
            Student student,
            ClassEntity classEntity
    ) {
        if (student == null || classEntity == null) {
            throw new IllegalArgumentException(
                    "Student and class are required"
            );
        }

        if (student.getSection() == null
                || student.getSection().getClassEntity() == null) {
            throw new SecurityException(
                    "Student is not assigned to a class"
            );
        }

        if (!classEntity.getId().equals(
                student.getSection().getClassEntity().getId())) {
            throw new SecurityException(
                    "Student does not belong to this class"
            );
        }

        if (classEntity.getSchool() == null
                || student.getSection().getClassEntity().getSchool() == null
                || !classEntity.getSchool().getId().equals(
                student.getSection().getClassEntity()
                        .getSchool().getId())) {
            throw new SecurityException(
                    "Student and class do not belong to the same school"
            );
        }
    }
    private AttendanceResponseDto mapToResponseDto(
            Attendance attendance
    ) {
        return AttendanceResponseDto.builder()
                .id(attendance.getId())
                .studentId(
                        attendance.getStudent() != null
                                ? attendance.getStudent().getId()
                                : null
                )
                .studentName(
                        attendance.getStudent() != null
                                ? attendance.getStudent().getName()
                                : null
                )
                .studentRollNumber(
                        attendance.getStudent() != null
                                ? attendance.getStudent().getRollNumber()
                                : null
                )
                .classId(
                        attendance.getClassEntity() != null
                                ? attendance.getClassEntity().getId()
                                : null
                )
                .className(
                        attendance.getClassEntity() != null
                                ? attendance.getClassEntity().getName()
                                : null
                )
                .markedByTeacherId(
                        attendance.getMarkedByTeacher() != null
                                ? attendance.getMarkedByTeacher().getId()
                                : null
                )
                .markedByTeacherName(
                        attendance.getMarkedByTeacher() != null
                                ? attendance.getMarkedByTeacher().getName()
                                : null
                )
                .attendanceDate(attendance.getAttendanceDate())
                .periodNumber(attendance.getPeriodNumber())
                .status(attendance.getStatus())
                .remarks(attendance.getRemarks())
                .markedAt(attendance.getMarkedAt())
                .updatedAt(attendance.getUpdatedAt())
                .updatedByTeacherId(
                        attendance.getUpdatedByTeacher() != null
                                ? attendance.getUpdatedByTeacher().getId()
                                : null
                )
                .updatedByTeacherName(
                        attendance.getUpdatedByTeacher() != null
                                ? attendance.getUpdatedByTeacher().getName()
                                : null
                )
                .build();
    }

    @Override
    public AttendanceDashboardSummaryDto getDashboardSummary() {

        Long schoolId = getCurrentSchoolId();
        LocalDate today = LocalDate.now();

        logger.info(
                "Generating attendance dashboard summary for school: {} on date: {}",
                schoolId,
                today
        );

        long totalStudents =
                studentRepository.countBySection_ClassEntity_School_Id(schoolId);

        long present = attendanceRepository
                .countByClassEntity_School_IdAndAttendanceDateAndStatus(
                        schoolId,
                        today,
                        "PRESENT"
                );

        long absent = attendanceRepository
                .countByClassEntity_School_IdAndAttendanceDateAndStatus(
                        schoolId,
                        today,
                        "ABSENT"
                );

        long leave = attendanceRepository
                .countByClassEntity_School_IdAndAttendanceDateAndStatus(
                        schoolId,
                        today,
                        "LEAVE"
                );

        long marked = attendanceRepository
                .countByClassEntity_School_IdAndAttendanceDate(
                        schoolId,
                        today
                );

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
    @Transactional(readOnly = true)
    public List<AttendanceLastSixDaysDto> getLastSixDaysAttendance() {

        LocalDate today = LocalDate.now();
        Long schoolId = getCurrentSchoolId();

        long totalStudents =
                studentRepository.countBySection_ClassEntity_School_Id(
                        schoolId
                );

        return java.util.stream.IntStream.rangeClosed(0, 5)
                .mapToObj(i -> {
                    LocalDate date = today.minusDays(5 - i);

                    long present =
                            attendanceRepository
                                    .countByClassEntity_School_IdAndAttendanceDateAndStatus(
                                            schoolId, date, "PRESENT"
                                    );

                    long absent =
                            attendanceRepository
                                    .countByClassEntity_School_IdAndAttendanceDateAndStatus(
                                            schoolId, date, "ABSENT"
                                    );

                    long leave =
                            attendanceRepository
                                    .countByClassEntity_School_IdAndAttendanceDateAndStatus(
                                            schoolId, date, "LEAVE"
                                    );

                    long marked =
                            attendanceRepository
                                    .countByClassEntity_School_IdAndAttendanceDate(
                                            schoolId, date
                                    );

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
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required"
            );
        }

        if (to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "End date must not precede start date"
            );
        }

        Long schoolId = getCurrentSchoolId();

        Page<Student> studentPage =
                studentRepository.findAllBySection_ClassEntity_School_Id(
                        schoolId, pageable
                );

        return studentPage.map(student -> {

            Long classId = null;
            String className = null;

            if (student.getSection() != null
                    && student.getSection().getClassEntity() != null) {

                classId = student.getSection().getClassEntity().getId();
                className = student.getSection().getClassEntity().getName();
            }

            long totalDays = 0;
            long presentDays = 0;
            long absentDays = 0;
            long leaveDays = 0;

            if (classId != null) {
                totalDays =
                        attendanceRepository.countByStudentClassSchoolAndDateRange(
                                student.getId(),
                                classId,
                                schoolId,
                                from,
                                to
                        );

                presentDays =
                        attendanceRepository.countByStudentClassSchoolDateRangeAndStatus(
                                student.getId(),
                                classId,
                                schoolId,
                                from,
                                to,
                                "PRESENT"
                        );

                absentDays =
                        attendanceRepository.countByStudentClassSchoolDateRangeAndStatus(
                                student.getId(),
                                classId,
                                schoolId,
                                from,
                                to,
                                "ABSENT"
                        );

                leaveDays =
                        attendanceRepository.countByStudentClassSchoolDateRangeAndStatus(
                                student.getId(),
                                classId,
                                schoolId,
                                from,
                                to,
                                "LEAVE"
                        );
            }

            double percentage = totalDays > 0
                    ? (presentDays * 100.0) / totalDays
                    : 0.0;

            percentage = Math.round(percentage * 100.0) / 100.0;

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
                    .attendancePercentage(percentage)
                    .build();
        });
    }


    @Override
    @Transactional
    public List<AttendanceResponseDto> updateBulkAttendance(
            BulkAttendanceUpdateRequestDto request
    ) {
        if (request == null
                || request.getAttendanceRecords() == null
                || request.getAttendanceRecords().isEmpty()) {
            throw new IllegalArgumentException(
                    "Attendance records are required"
            );
        }

        Long schoolId = getCurrentSchoolId();
        boolean admin = isSchoolAdmin();

        logger.info(
                "Updating {} attendance records for school: {}",
                request.getAttendanceRecords().size(),
                schoolId
        );

        Teacher teacher = null;

        if (!admin) {
            if (!hasRole("TEACHER")) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Only school admins and authorized teachers can update attendance"
                );
            }

            teacher = teacherRepository
                    .findByUser_UsernameAndSchool_Id(
                            getCurrentUsername(),
                            schoolId
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Teacher profile not found for the current school"
                            )
                    );

            if (request.getTeacherId() != null
                    && !request.getTeacherId().equals(teacher.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You cannot update attendance as another teacher"
                );
            }
        }

        List<AttendanceResponseDto> responses = new ArrayList<>();

        Set<Long> processedAttendanceIds = new HashSet<>();

        for (var record : request.getAttendanceRecords()) {

            if (record == null || record.getAttendanceId() == null) {
                throw new IllegalArgumentException(
                        "Attendance ID is required for every record"
                );
            }

            if (!processedAttendanceIds.add(record.getAttendanceId())) {
                throw new IllegalArgumentException(
                        "Duplicate attendance ID in request: "
                                + record.getAttendanceId()
                );
            }

            String status = normalizeStatus(record.getStatus());

            Attendance attendance = attendanceRepository
                    .findByIdAndClassEntity_School_Id(
                            record.getAttendanceId(),
                            schoolId
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Attendance not found in the current school with id: "
                                            + record.getAttendanceId()
                            )
                    );

            if (attendance.getClassEntity() == null) {
                throw new IllegalStateException(
                        "Attendance record has no associated class"
                );
            }

            if (!admin) {
                teacherAccessService.validateTeacherAccessToClass(
                        teacher.getId(),
                        attendance.getClassEntity().getId()
                );
            }

            attendance.setStatus(status);
            attendance.setRemarks(record.getRemarks());

            if (teacher != null) {
                attendance.setUpdatedByTeacher(teacher);
            }

            Attendance saved = attendanceRepository.save(attendance);
            responses.add(mapToResponseDto(saved));
        }

        return responses;
    }



    private Integer resolvePeriodNumber(
            ClassEntity classEntity,
            Integer requestedPeriodNumber
    ) {
        if (classEntity.getSchool() == null) {
            throw new IllegalStateException(
                    "School configuration not found for this class"
            );
        }

        String mode = classEntity.getSchool().getAttendanceMode();

        if (mode == null || mode.isBlank()) {
            mode = "SINGLE";
        }

        mode = mode.trim().toUpperCase(Locale.ROOT);

        if ("SINGLE".equals(mode)) {
            // Daily attendance is stored with period number 0.
            return 0;
        }

        if (!"PERIOD_WISE".equals(mode)) {
            throw new IllegalStateException(
                    "Invalid attendance mode configured: " + mode
            );
        }

        if (requestedPeriodNumber == null) {
            throw new IllegalArgumentException(
                    "Period number is required for period-wise attendance"
            );
        }

        Integer configuredPeriodCount =
                classEntity.getSchool().getAttendancePeriodCount();

        if (configuredPeriodCount == null
                || configuredPeriodCount < 1
                || configuredPeriodCount > 20) {
            throw new IllegalStateException(
                    "Invalid attendance period count configured for this school"
            );
        }

        if (requestedPeriodNumber < 1
                || requestedPeriodNumber > configuredPeriodCount) {
            throw new IllegalArgumentException(
                    "Period number must be between 1 and "
                            + configuredPeriodCount
            );
        }

        return requestedPeriodNumber;
    }

}



