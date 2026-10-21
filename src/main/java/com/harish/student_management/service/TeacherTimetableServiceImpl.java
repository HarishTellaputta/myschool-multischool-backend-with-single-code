package com.harish.student_management.service;

import com.harish.student_management.dto.TeacherTimetableRequestDto;
import com.harish.student_management.dto.TeacherTimetableResponseDto;
import com.harish.student_management.dto.TimetableImportErrorDto;
import com.harish.student_management.dto.TimetableImportResponseDto;
import com.harish.student_management.entity.ClassEntity;
import com.harish.student_management.entity.Section;
import com.harish.student_management.entity.Subject;
import com.harish.student_management.entity.Teacher;
import com.harish.student_management.entity.TeacherTimetable;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.ClassRepository;
import com.harish.student_management.repository.ClassSubjectRepository;
import com.harish.student_management.repository.SectionRepository;
import com.harish.student_management.repository.StudentRepository;
import com.harish.student_management.repository.SubjectRepository;
import com.harish.student_management.repository.TeacherClassAssignmentRepository;
import com.harish.student_management.repository.TeacherRepository;
import com.harish.student_management.repository.TeacherTimetableRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherTimetableServiceImpl
        implements TeacherTimetableService {

    private static final Logger logger =
            LoggerFactory.getLogger(TeacherTimetableServiceImpl.class);

    private final TeacherTimetableRepository timetableRepository;

    private final TeacherRepository teacherRepository;

    private final ClassRepository classRepository;

    private final SectionRepository sectionRepository;

    private final StudentRepository studentRepository;

    private final SubjectRepository subjectRepository;

    private final ClassSubjectRepository classSubjectRepository;

    private final TeacherClassAssignmentRepository assignmentRepository;
    private final CurrentSchoolService currentSchoolService;


    // =========================================================
    // CREATE TIMETABLE
    // =========================================================

    @Override
    public TeacherTimetableResponseDto createTimetableEntry(
            TeacherTimetableRequestDto request) {

        logger.info(
                "Creating timetable entry for teacher: {}",
                request.getTeacherId()
        );

        validateRequest(request);

        Teacher teacher =
                teacherRepository.findById(
                        request.getTeacherId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher not found with id: "
                                        + request.getTeacherId()
                        )
                );

        ClassEntity classEntity =
                classRepository.findById(
                        request.getClassId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Class not found with id: "
                                        + request.getClassId()
                        )
                );

        Subject subject =
                subjectRepository.findById(
                        request.getSubjectId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Subject not found with id: "
                                        + request.getSubjectId()
                        )
                );

        Section section =
                getSection(request.getSectionId());

        // -----------------------------------------------------
        // SCHOOL VALIDATION
        // -----------------------------------------------------

        validateSchoolRelationship(
                teacher,
                classEntity,
                subject
        );

        // -----------------------------------------------------
        // SECTION VALIDATION
        // -----------------------------------------------------

        validateSection(
                section,
                classEntity.getId()
        );

        // -----------------------------------------------------
        // CLASS + SUBJECT MAPPING
        // -----------------------------------------------------

        validateClassSubject(
                classEntity.getId(),
                subject.getId()
        );

        // -----------------------------------------------------
        // TEACHER + CLASS + SUBJECT ASSIGNMENT
        // -----------------------------------------------------

        validateTeacherAssignment(
                teacher.getId(),
                classEntity.getId(),
                subject.getId()
        );

        // -----------------------------------------------------
        // TIME VALIDATION
        // -----------------------------------------------------

        validateTimeRange(request);

        // -----------------------------------------------------
        // CONFLICT VALIDATION
        // -----------------------------------------------------

        validateConflict(
                null,
                teacher.getId(),
                classEntity.getId(),
                section,
                request
        );

        // -----------------------------------------------------
        // BUILD ENTITY
        // -----------------------------------------------------

        TeacherTimetable timetable =
                TeacherTimetable.builder()
                        .teacher(teacher)
                        .classEntity(classEntity)
                        .section(section)
                        .subject(subject)
                        .dayOfWeek(request.getDayOfWeek())
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .roomNumber(request.getRoomNumber())
                        .build();

        TeacherTimetable savedTimetable =
                timetableRepository.save(timetable);

        logger.info(
                "Timetable entry created successfully with ID: {}",
                savedTimetable.getId()
        );

        return mapToResponseDto(savedTimetable);
    }


    // =========================================================
    // GET BY ID
    // =========================================================

    @Override
    public TeacherTimetableResponseDto getTimetableById(Long id) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        TeacherTimetable timetable =
                timetableRepository.findByIdAndSchoolId(id, schoolId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Timetable entry not found in current school: " + id));

        return mapToResponseDto(timetable);
    }

    // =========================================================
    // GET BY TEACHER
    // =========================================================

    @Override
    public List<TeacherTimetableResponseDto> getTimetableByTeacherId(
            Long teacherId) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        logger.info(
                "Fetching timetable for teacher ID: {} and school ID: {}",
                teacherId,
                schoolId
        );

        if (!teacherRepository.existsByIdAndSchool_Id(teacherId, schoolId)) {
            throw new IllegalArgumentException(
                    "Teacher not found in the current school"
            );
        }

        return timetableRepository
                .findByTeacherIdAndSchoolId(teacherId, schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================
    // GET BY TEACHER + DAY
    // =========================================================

    @Override
    public List<TeacherTimetableResponseDto>
    getTimetableByTeacherIdAndDayOfWeek(
            Long teacherId,
            String dayOfWeek) {

        if (dayOfWeek == null || dayOfWeek.isBlank()) {
            throw new IllegalArgumentException(
                    "Day of week is required"
            );
        }

        DayOfWeek day;

        try {
            day = DayOfWeek.valueOf(
                    dayOfWeek.trim().toUpperCase()
            );
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid day of week: " + dayOfWeek
            );
        }

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return timetableRepository
                .findByTeacherIdAndDayOfWeekAndSchoolId(
                        teacherId,
                        day,
                        schoolId
                )
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================
    // GET BY CLASS
    // =========================================================

    @Override
    public List<TeacherTimetableResponseDto> getTimetableByClassId(
            Long classId) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return timetableRepository
                .findByClassIdAndSchoolId(classId, schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }
    // =========================================================
    // UPDATE TIMETABLE
    // =========================================================

    @Override
    public TeacherTimetableResponseDto updateTimetable(
            Long id,
            TeacherTimetableRequestDto request) {

        logger.info(
                "Updating timetable entry with ID: {}",
                id
        );

        validateRequest(request);

        TeacherTimetable timetable =
                timetableRepository.findByIdAndSchoolId(
                                id,
                                currentSchoolService.getCurrentSchoolId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Timetable entry not found with id: "
                                                + id
                                )
                        );

        Teacher teacher =
                teacherRepository.findById(
                        request.getTeacherId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher not found with id: "
                                        + request.getTeacherId()
                        )
                );

        ClassEntity classEntity =
                classRepository.findById(
                        request.getClassId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Class not found with id: "
                                        + request.getClassId()
                        )
                );

        Subject subject =
                subjectRepository.findById(
                        request.getSubjectId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Subject not found with id: "
                                        + request.getSubjectId()
                        )
                );

        Section section =
                getSection(request.getSectionId());

        // -----------------------------------------------------
        // SCHOOL VALIDATION
        // -----------------------------------------------------

        validateSchoolRelationship(
                teacher,
                classEntity,
                subject
        );

        // -----------------------------------------------------
        // SECTION VALIDATION
        // -----------------------------------------------------

        validateSection(
                section,
                classEntity.getId()
        );

        // -----------------------------------------------------
        // CLASS + SUBJECT MAPPING
        // -----------------------------------------------------

        validateClassSubject(
                classEntity.getId(),
                subject.getId()
        );

        // -----------------------------------------------------
        // TEACHER + CLASS + SUBJECT ASSIGNMENT
        // -----------------------------------------------------

        validateTeacherAssignment(
                teacher.getId(),
                classEntity.getId(),
                subject.getId()
        );

        // -----------------------------------------------------
        // TIME VALIDATION
        // -----------------------------------------------------

        validateTimeRange(request);

        // -----------------------------------------------------
        // CONFLICT VALIDATION
        // -----------------------------------------------------

        validateConflict(
                id,
                teacher.getId(),
                classEntity.getId(),
                section,
                request
        );

        // -----------------------------------------------------
        // UPDATE ENTITY
        // -----------------------------------------------------

        timetable.setTeacher(teacher);

        timetable.setClassEntity(classEntity);

        timetable.setSection(section);

        timetable.setSubject(subject);

        timetable.setDayOfWeek(
                request.getDayOfWeek()
        );

        timetable.setStartTime(
                request.getStartTime()
        );

        timetable.setEndTime(
                request.getEndTime()
        );

        timetable.setRoomNumber(
                request.getRoomNumber()
        );

        TeacherTimetable updatedTimetable =
                timetableRepository.save(timetable);

        logger.info(
                "Timetable entry updated successfully with ID: {}",
                updatedTimetable.getId()
        );

        return mapToResponseDto(updatedTimetable);
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Override
    public void deleteTimetable(Long id) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        TeacherTimetable timetable =
                timetableRepository.findByIdAndSchoolId(id, schoolId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Timetable entry not found in current school: " + id));

        timetableRepository.delete(timetable);
    }

    // =========================================================
    // GET TIMETABLE BY STUDENT
    // =========================================================

    @Override
    public List<TeacherTimetableResponseDto>
    getTimetableByStudentId(Long studentId) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        var student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student not found: " + studentId
                        )
                );

        if (student.getSection() == null
                || student.getSection().getClassEntity() == null) {
            return List.of();
        }

        var classEntity = student.getSection().getClassEntity();

        if (classEntity.getSchool() == null
                || !schoolId.equals(classEntity.getSchool().getId())) {
            throw new ResourceNotFoundException(
                    "Student not found in current school: " + studentId
            );
        }

        Long classId = classEntity.getId();
        Long sectionId = student.getSection().getId();

        return timetableRepository
                .findByClassIdAndSchoolId(classId, schoolId)
                .stream()
                .filter(timetable ->
                        timetable.getSection() == null
                                || timetable.getSection().getId().equals(sectionId)
                )
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================
    // GET SECTION
    // =========================================================

    private Section getSection(Long sectionId) {

        if (sectionId == null) {
            return null;
        }

        return sectionRepository
                .findById(sectionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Section not found with id: "
                                        + sectionId
                        )
                );
    }


    // =========================================================
    // VALIDATE REQUEST
    // =========================================================

    private void validateRequest(
            TeacherTimetableRequestDto request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Timetable request cannot be null"
            );
        }

        if (request.getTeacherId() == null) {
            throw new IllegalArgumentException(
                    "Teacher ID is required"
            );
        }

        if (request.getClassId() == null) {
            throw new IllegalArgumentException(
                    "Class ID is required"
            );
        }

        if (request.getSubjectId() == null) {
            throw new IllegalArgumentException(
                    "Subject ID is required"
            );
        }

        if (request.getSectionId() == null) {
            throw new IllegalArgumentException(
                    "Section ID is required"
            );
        }

        if (request.getDayOfWeek() == null) {
            throw new IllegalArgumentException(
                    "Day of week is required"
            );
        }

        if (request.getStartTime() == null) {
            throw new IllegalArgumentException(
                    "Start time is required"
            );
        }

        if (request.getEndTime() == null) {
            throw new IllegalArgumentException(
                    "End time is required"
            );
        }
    }


    // =========================================================
    // VALIDATE SCHOOL RELATIONSHIP
    // =========================================================

    private void validateSchoolRelationship(
            Teacher teacher,
            ClassEntity classEntity,
            Subject subject) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        if (teacher.getSchool() == null
                || classEntity.getSchool() == null
                || subject.getSchool() == null) {
            throw new IllegalArgumentException(
                    "Teacher, Class and Subject must belong to a school"
            );
        }

        Long teacherSchoolId = teacher.getSchool().getId();
        Long classSchoolId = classEntity.getSchool().getId();
        Long subjectSchoolId = subject.getSchool().getId();

        if (schoolId == null
                || !schoolId.equals(teacherSchoolId)
                || !schoolId.equals(classSchoolId)
                || !schoolId.equals(subjectSchoolId)) {
            throw new ResourceNotFoundException(
                    "Teacher, Class or Subject not found in current school"
            );
        }
    }


    // =========================================================
    // VALIDATE CLASS + SUBJECT MAPPING
    // =========================================================

    private void validateClassSubject(
            Long classId,
            Long subjectId) {

        boolean mapped =
                classSubjectRepository
                        .existsByClassEntityIdAndSubjectId(
                                classId,
                                subjectId
                        );

        if (!mapped) {

            throw new IllegalArgumentException(
                    "Subject is not assigned to this class"
            );
        }
    }


    // =========================================================
    // VALIDATE TEACHER + CLASS + SUBJECT ASSIGNMENT
    // =========================================================

    private void validateTeacherAssignment(
            Long teacherId,
            Long classId,
            Long subjectId) {

        boolean assigned =
                assignmentRepository
                        .existsByTeacherIdAndClassEntityIdAndSubjectId(
                                teacherId,
                                classId,
                                subjectId
                        );

        if (!assigned) {

            throw new IllegalArgumentException(
                    "Teacher is not assigned to this class for the selected subject"
            );
        }
    }


    // =========================================================
    // VALIDATE SECTION
    // =========================================================

    private void validateSection(
            Section section,
            Long classId) {

        if (section == null) {
            return;
        }

        if (section.getClassEntity() == null) {

            throw new IllegalArgumentException(
                    "Section is not associated with a class"
            );
        }

        if (!section.getClassEntity()
                .getId()
                .equals(classId)) {

            throw new IllegalArgumentException(
                    "Section does not belong to selected class"
            );
        }
    }


    // =========================================================
    // VALIDATE TIME
    // =========================================================

    private void validateTimeRange(
            TeacherTimetableRequestDto request) {

        if (request.getStartTime() == null
                || request.getEndTime() == null) {

            throw new IllegalArgumentException(
                    "Start time and end time are required"
            );
        }

        if (!request.getEndTime()
                .isAfter(request.getStartTime())) {

            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }
    }


    // =========================================================
    // VALIDATE CONFLICT
    // =========================================================

    private void validateConflict(
            Long id,
            Long teacherId,
            Long classId,
            Section section,
            TeacherTimetableRequestDto request) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        // Existing teacher conflict and class conflict logic continues here.

        // -----------------------------------------------------
        // TEACHER CONFLICT
        // -----------------------------------------------------

        List<TeacherTimetable> teacherEntries =
                timetableRepository.findByTeacherIdAndDayOfWeekAndSchoolId(
                        teacherId,
                        request.getDayOfWeek(),
                        schoolId);

        boolean teacherConflict =
                teacherEntries
                        .stream()
                        .anyMatch(entry ->

                                isDifferentEntry(
                                        entry,
                                        id
                                )

                                        && overlaps(
                                        entry.getStartTime(),
                                        entry.getEndTime(),
                                        request.getStartTime(),
                                        request.getEndTime()
                                )
                        );

        if (teacherConflict) {

            throw new IllegalArgumentException(
                    "Teacher has a conflicting timetable entry"
            );
        }


        // -----------------------------------------------------
        // CLASS / SECTION CONFLICT
        // -----------------------------------------------------

        List<TeacherTimetable> classEntries =
                timetableRepository.findByClassIdAndSchoolId(
                                classId,
                                schoolId)
                        .stream()
                        .filter(entry ->
                                entry.getDayOfWeek() == request.getDayOfWeek())
                        .filter(entry ->
                                isSameSectionScope(entry, section))
                        .toList();

        boolean classConflict =
                classEntries
                        .stream()
                        .anyMatch(entry ->

                                isDifferentEntry(
                                        entry,
                                        id
                                )

                                        && overlaps(
                                        entry.getStartTime(),
                                        entry.getEndTime(),
                                        request.getStartTime(),
                                        request.getEndTime()
                                )
                        );

        if (classConflict) {

            throw new IllegalArgumentException(
                    "Class/section has a conflicting timetable entry"
            );
        }
    }


    // =========================================================
    // CHECK SAME SECTION SCOPE
    // =========================================================

    private boolean isSameSectionScope(
            TeacherTimetable entry,
            Section requestedSection) {

        // Existing timetable is for all sections
        if (entry.getSection() == null) {
            return true;
        }

        // Requested timetable is for all sections
        if (requestedSection == null) {
            return true;
        }

        return entry.getSection()
                .getId()
                .equals(requestedSection.getId());
    }


    // =========================================================
    // CHECK CURRENT RECORD
    // =========================================================

    private boolean isDifferentEntry(
            TeacherTimetable entry,
            Long id) {

        if (id == null) {
            return true;
        }

        return entry.getId() == null
                || !entry.getId().equals(id);
    }


    // =========================================================
    // TIME OVERLAP
    // =========================================================

    private boolean overlaps(
            java.time.LocalTime startA,
            java.time.LocalTime endA,
            java.time.LocalTime startB,
            java.time.LocalTime endB) {

        if (startA == null
                || endA == null
                || startB == null
                || endB == null) {

            return false;
        }

        return startA.isBefore(endB)
                && startB.isBefore(endA);
    }


    // =========================================================
    // ENTITY -> RESPONSE DTO
    // =========================================================

    private TeacherTimetableResponseDto mapToResponseDto(
            TeacherTimetable timetable) {

        return TeacherTimetableResponseDto.builder()

                .id(
                        timetable.getId()
                )

                // -------------------------------------------------
                // TEACHER
                // -------------------------------------------------

                .teacherId(
                        timetable.getTeacher() != null
                                ? timetable.getTeacher().getId()
                                : null
                )

                .teacherName(
                        timetable.getTeacher() != null
                                ? timetable.getTeacher().getName()
                                : null
                )

                // -------------------------------------------------
                // CLASS
                // -------------------------------------------------

                .classId(
                        timetable.getClassEntity() != null
                                ? timetable.getClassEntity().getId()
                                : null
                )

                .className(
                        timetable.getClassEntity() != null
                                ? timetable.getClassEntity().getName()
                                : null
                )

                // -------------------------------------------------
                // SECTION
                // -------------------------------------------------

                .sectionId(
                        timetable.getSection() != null
                                ? timetable.getSection().getId()
                                : null
                )

                .sectionName(
                        timetable.getSection() != null
                                ? timetable.getSection().getName()
                                : null
                )

                // -------------------------------------------------
                // SUBJECT MASTER
                // -------------------------------------------------

                .subjectId(
                        timetable.getSubject() != null
                                ? timetable.getSubject().getId()
                                : null
                )

                .subjectName(
                        timetable.getSubject() != null
                                ? timetable.getSubject().getName()
                                : null
                )

                .subjectCode(
                        timetable.getSubject() != null
                                ? timetable.getSubject().getCode()
                                : null
                )

                // -------------------------------------------------
                // TIMING
                // -------------------------------------------------

                .dayOfWeek(
                        timetable.getDayOfWeek()
                )

                .startTime(
                        timetable.getStartTime()
                )

                .endTime(
                        timetable.getEndTime()
                )

                .roomNumber(
                        timetable.getRoomNumber()
                )

                // -------------------------------------------------
                // AUDIT
                // -------------------------------------------------

                .createdAt(
                        timetable.getCreatedAt()
                )

                .updatedAt(
                        timetable.getUpdatedAt()
                )

                .build();
    }


    // =========================================================
// EXCEL IMPORT
// =========================================================

    @Override
    public TimetableImportResponseDto importFromExcel(
            MultipartFile file) {

        logger.info("Starting timetable Excel import");

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Excel file is required"
            );
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null
                || !fileName.toLowerCase().endsWith(".xlsx")) {

            throw new IllegalArgumentException(
                    "Only .xlsx Excel files are supported"
            );
        }

        int totalRows = 0;
        int successCount = 0;
        int failedCount = 0;

        List<TimetableImportErrorDto> errors =
                new ArrayList<>();

        try (Workbook workbook =
                     WorkbookFactory.create(file.getInputStream())) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException(
                        "Excel file does not contain a sheet"
                );
            }

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() <= 1) {
                throw new IllegalArgumentException(
                        "Excel file does not contain timetable data"
                );
            }

            // -----------------------------------------------------
            // HEADER VALIDATION
            // -----------------------------------------------------

            Row headerRow = sheet.getRow(0);

            validateExcelHeaders(headerRow);

            // -----------------------------------------------------
            // READ DATA ROWS
            // -----------------------------------------------------

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                // Skip completely empty rows
                if (isEmptyRow(row)) {
                    continue;
                }

                totalRows++;

                int excelRowNumber = rowIndex + 1;

                try {

                    TeacherTimetableRequestDto request =
                            parseTimetableRow(row);

                    // -------------------------------------------------
                    // USE EXISTING VALIDATION + CREATE LOGIC
                    // -------------------------------------------------

                    importOrUpdateTimetableEntry(request);

                    successCount++;

                } catch (Exception ex) {

                    failedCount++;

                    logger.warn(
                            "Timetable import failed at row {}: {}",
                            excelRowNumber,
                            ex.getMessage()
                    );

                    errors.add(
                            TimetableImportErrorDto.builder()
                                    .row(excelRowNumber)
                                    .message(
                                            ex.getMessage() != null
                                                    ? ex.getMessage()
                                                    : "Invalid timetable data"
                                    )
                                    .build()
                    );
                }
            }

        } catch (IOException ex) {

            logger.error(
                    "Failed to read timetable Excel file",
                    ex
            );

            throw new IllegalArgumentException(
                    "Unable to read Excel file"
            );

        } catch (IllegalArgumentException ex) {

            throw ex;

        } catch (Exception ex) {

            logger.error(
                    "Unexpected error during timetable Excel import",
                    ex
            );

            throw new IllegalArgumentException(
                    "Unable to process Excel file"
            );
        }

        logger.info(
                "Timetable Excel import completed. Total: {}, Success: {}, Failed: {}",
                totalRows,
                successCount,
                failedCount
        );

        return TimetableImportResponseDto.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .failedCount(failedCount)
                .errors(errors)
                .build();
    }
// =========================================================
// CREATE OR UPDATE TIMETABLE FROM EXCEL
// =========================================================

    private TeacherTimetableResponseDto importOrUpdateTimetableEntry(
            TeacherTimetableRequestDto request) {

        validateRequest(request);

        Teacher teacher =
                teacherRepository.findById(
                        request.getTeacherId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher not found with id: "
                                        + request.getTeacherId()
                        )
                );

        ClassEntity classEntity =
                classRepository.findById(
                        request.getClassId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Class not found with id: "
                                        + request.getClassId()
                        )
                );

        Subject subject =
                subjectRepository.findById(
                        request.getSubjectId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Subject not found with id: "
                                        + request.getSubjectId()
                        )
                );

        Section section =
                getSection(request.getSectionId());

        // ---------------------------------------------------------
        // VALIDATIONS
        // ---------------------------------------------------------

        validateSchoolRelationship(
                teacher,
                classEntity,
                subject
        );

        validateSection(
                section,
                classEntity.getId()
        );

        validateClassSubject(
                classEntity.getId(),
                subject.getId()
        );

        validateTeacherAssignment(
                teacher.getId(),
                classEntity.getId(),
                subject.getId()
        );

        validateTimeRange(request);

        // ---------------------------------------------------------
        // FIND EXISTING SLOT
        // ---------------------------------------------------------

        TeacherTimetable existing =
                findExistingSlot(
                        classEntity.getId(),
                        section.getId(),
                        request.getDayOfWeek(),
                        request.getStartTime(),
                        request.getEndTime()
                );

        Long existingId =
                existing != null
                        ? existing.getId()
                        : null;

        // ---------------------------------------------------------
        // CONFLICT VALIDATION
        // ---------------------------------------------------------

        validateConflict(
                existingId,
                teacher.getId(),
                classEntity.getId(),
                section,
                request
        );

        // ---------------------------------------------------------
        // UPDATE EXISTING
        // ---------------------------------------------------------

        if (existing != null) {

            existing.setTeacher(teacher);

            existing.setClassEntity(classEntity);

            existing.setSection(section);

            existing.setSubject(subject);

            existing.setDayOfWeek(
                    request.getDayOfWeek()
            );

            existing.setStartTime(
                    request.getStartTime()
            );

            existing.setEndTime(
                    request.getEndTime()
            );

            existing.setRoomNumber(
                    request.getRoomNumber()
            );

            TeacherTimetable updated =
                    timetableRepository.save(existing);

            logger.info(
                    "Timetable slot updated successfully. ID: {}",
                    updated.getId()
            );

            return mapToResponseDto(updated);
        }

        // ---------------------------------------------------------
        // CREATE NEW
        // ---------------------------------------------------------

        TeacherTimetable timetable =
                TeacherTimetable.builder()
                        .teacher(teacher)
                        .classEntity(classEntity)
                        .section(section)
                        .subject(subject)
                        .dayOfWeek(
                                request.getDayOfWeek()
                        )
                        .startTime(
                                request.getStartTime()
                        )
                        .endTime(
                                request.getEndTime()
                        )
                        .roomNumber(
                                request.getRoomNumber()
                        )
                        .build();

        TeacherTimetable saved =
                timetableRepository.save(timetable);

        logger.info(
                "New timetable slot created successfully. ID: {}",
                saved.getId()
        );

        return mapToResponseDto(saved);
    }
    // =========================================================
// FIND EXISTING TIMETABLE SLOT
// =========================================================

    private TeacherTimetable findExistingSlot(
            Long classId,
            Long sectionId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime) {

        return timetableRepository.
        findByClassIdAndSchoolId(
                classId,
                currentSchoolService.getCurrentSchoolId()
        )
                .stream()
                .filter(entry ->
                        entry.getSection() != null
                                && entry.getSection()
                                .getId()
                                .equals(sectionId)
                )
                .filter(entry ->
                        entry.getDayOfWeek()
                                == dayOfWeek
                )
                .filter(entry ->
                        entry.getStartTime()
                                .equals(startTime)
                )
                .filter(entry ->
                        entry.getEndTime()
                                .equals(endTime)
                )
                .findFirst()
                .orElse(null);
    }
    private TeacherTimetableRequestDto parseTimetableRow(Row row) {

        String teacherName =
                getRequiredString(
                        row.getCell(0),
                        "Teacher"
                );

        String className =
                getRequiredString(
                        row.getCell(1),
                        "Class"
                );

        String sectionName =
                getRequiredString(
                        row.getCell(2),
                        "Section"
                );

        String subjectName =
                getRequiredString(
                        row.getCell(3),
                        "Subject"
                );

        String dayValue =
                getRequiredString(
                        row.getCell(4),
                        "Day"
                );

        String startTimeValue =
                getRequiredString(
                        row.getCell(5),
                        "Start Time"
                );

        String endTimeValue =
                getRequiredString(
                        row.getCell(6),
                        "End Time"
                );

        String roomNumber =
                getOptionalString(
                        row.getCell(7)
                );

        // -----------------------------------------------------
        // FIND ENTITIES BY NAME
        // -----------------------------------------------------

        Teacher teacher =
                findTeacherByName(teacherName);

        ClassEntity classEntity =
                findClassByName(className);

        Subject subject =
                findSubjectByName(subjectName);

        Section section =
                findSectionByName(
                        sectionName,
                        classEntity.getId()
                );

        // -----------------------------------------------------
        // VALIDATE SCHOOL RELATIONSHIP
        // -----------------------------------------------------

        validateSchoolRelationship(
                teacher,
                classEntity,
                subject
        );

        // -----------------------------------------------------
        // VALIDATE SECTION
        // -----------------------------------------------------

        validateSection(
                section,
                classEntity.getId()
        );

        // -----------------------------------------------------
        // DAY
        // -----------------------------------------------------

        DayOfWeek dayOfWeek;

        try {

            dayOfWeek =
                    DayOfWeek.valueOf(
                            dayValue
                                    .trim()
                                    .toUpperCase()
                    );

        } catch (IllegalArgumentException ex) {

            throw new IllegalArgumentException(
                    "Invalid day: "
                            + dayValue
                            + ". Use MONDAY, TUESDAY, etc."
            );
        }

        // -----------------------------------------------------
        // TIME
        // -----------------------------------------------------

        LocalTime startTime =
                parseExcelTime(
                        row.getCell(5),
                        startTimeValue
                );

        LocalTime endTime =
                parseExcelTime(
                        row.getCell(6),
                        endTimeValue
                );

        // -----------------------------------------------------
        // BUILD REQUEST
        // -----------------------------------------------------

        return TeacherTimetableRequestDto.builder()
                .teacherId(teacher.getId())
                .classId(classEntity.getId())
                .sectionId(section.getId())
                .subjectId(subject.getId())
                .dayOfWeek(dayOfWeek)
                .startTime(startTime)
                .endTime(endTime)
                .roomNumber(roomNumber)
                .build();
    }
    private String getCellString(Cell cell) {

        if (cell == null) {
            return "";
        }

        DataFormatter formatter =
                new DataFormatter();

        return formatter
                .formatCellValue(cell)
                .trim();
    }
    private String getRequiredString(
            Cell cell,
            String fieldName) {

        String value =
                getCellString(cell);

        if (value.isBlank()) {

            throw new IllegalArgumentException(
                    fieldName + " is required"
            );
        }

        return value;
    }

    private String getOptionalString(Cell cell) {

        String value =
                getCellString(cell);

        return value.isBlank()
                ? null
                : value;
    }

    private Long getRequiredLong(
            Cell cell,
            String fieldName) {

        String value =
                getRequiredString(
                        cell,
                        fieldName
                );

        try {

            return Long.valueOf(
                    value
            );

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                    fieldName
                            + " must be a valid number"
            );
        }
    }

    private Long getOptionalLong(Cell cell) {

        String value =
                getOptionalString(cell);

        if (value == null) {
            return null;
        }

        try {

            return Long.valueOf(value);

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                    "Section ID must be a valid number"
            );
        }
    }

    private LocalTime parseExcelTime(
            Cell cell,
            String value) {

        // Excel native time
        if (cell != null
                && cell.getCellType()
                == CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {

            return cell
                    .getLocalDateTimeCellValue()
                    .toLocalTime();
        }

        // Text such as 09:00
        try {

            return LocalTime.parse(
                    value.trim()
            );

        } catch (Exception ex) {

            throw new IllegalArgumentException(
                    "Invalid time: "
                            + value
                            + ". Use HH:mm format"
            );
        }
    }

    private boolean isEmptyRow(Row row) {

        if (row == null) {
            return true;
        }

        for (int i = 0; i < 8; i++) {

            Cell cell =
                    row.getCell(i);

            if (cell != null
                    && !getCellString(cell).isBlank()) {

                return false;
            }
        }

        return true;
    }

    private void validateExcelHeaders(Row headerRow) {

        if (headerRow == null) {
            throw new IllegalArgumentException(
                    "Excel header row is missing"
            );
        }

        String[] expectedHeaders = {
                "Teacher",
                "Class",
                "Section",
                "Subject",
                "Day",
                "Start Time",
                "End Time",
                "Room Number"
        };

        for (int i = 0; i < expectedHeaders.length; i++) {

            String actual =
                    getCellString(headerRow.getCell(i));

            if (!expectedHeaders[i]
                    .equalsIgnoreCase(actual.trim())) {

                throw new IllegalArgumentException(
                        "Invalid Excel header at column "
                                + (i + 1)
                                + ". Expected: "
                                + expectedHeaders[i]
                );
            }
        }
    }

    private Teacher findTeacherByName(String teacherName) {

        List<Teacher> teachers =
                teacherRepository.findAll()
                        .stream()
                        .filter(teacher ->
                                teacher.getName() != null
                                        && teacher.getName()
                                        .trim()
                                        .equalsIgnoreCase(
                                                teacherName.trim()
                                        )
                        )
                        .toList();

        if (teachers.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Teacher not found with name: "
                            + teacherName
            );
        }

        if (teachers.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple teachers found with name: "
                            + teacherName
                            + ". Please use a unique teacher name."
            );
        }

        return teachers.get(0);
    }

    private ClassEntity findClassByName(String className) {

        List<ClassEntity> classes =
                classRepository.findAll()
                        .stream()
                        .filter(classEntity ->
                                classEntity.getName() != null
                                        && classEntity.getName()
                                        .trim()
                                        .equalsIgnoreCase(
                                                className.trim()
                                        )
                        )
                        .toList();

        if (classes.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Class not found with name: "
                            + className
            );
        }

        if (classes.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple classes found with name: "
                            + className
                            + ". Please use a unique class name."
            );
        }

        return classes.get(0);
    }

    private Subject findSubjectByName(String subjectName) {

        List<Subject> subjects =
                subjectRepository.findAll()
                        .stream()
                        .filter(subject ->
                                subject.getName() != null
                                        && subject.getName()
                                        .trim()
                                        .equalsIgnoreCase(
                                                subjectName.trim()
                                        )
                        )
                        .toList();

        if (subjects.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Subject not found with name: "
                            + subjectName
            );
        }

        if (subjects.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple subjects found with name: "
                            + subjectName
                            + ". Please use a unique subject name."
            );
        }

        return subjects.get(0);
    }


    private Section findSectionByName(
            String sectionName,
            Long classId) {

        List<Section> sections =
                sectionRepository.findAll()
                        .stream()
                        .filter(section ->
                                section.getName() != null
                                        && section.getName()
                                        .trim()
                                        .equalsIgnoreCase(
                                                sectionName.trim()
                                        )
                        )
                        .filter(section ->
                                section.getClassEntity() != null
                                        && section.getClassEntity()
                                        .getId()
                                        .equals(classId)
                        )
                        .toList();

        if (sections.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Section '"
                            + sectionName
                            + "' not found for class ID: "
                            + classId
            );
        }

        if (sections.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple sections found with name: "
                            + sectionName
                            + " for class ID: "
                            + classId
            );
        }

        return sections.get(0);
    }






}