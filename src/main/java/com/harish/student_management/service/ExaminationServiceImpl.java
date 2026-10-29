
        package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.*;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExaminationServiceImpl implements ExaminationService {

    private final ExaminationRepository examinationRepository;
    private final ExamScheduleRepository scheduleRepository;
    private final ExamResultRepository resultRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final TeacherAccessService teacherAccessService;
    private final GradeRuleRepository gradeRuleRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final CurrentSchoolService currentSchoolService;


    @Override
    @Transactional
    public ExaminationResponseDto createExamination(
            ExaminationRequestDto r) {

        if (r == null) {
            throw new IllegalArgumentException(
                    "Examination request is required");
        }

        if (r.getAcademicYearId() == null) {
            throw new IllegalArgumentException(
                    "Academic year ID is required");
        }

        if (r.getName() == null || r.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Examination name is required");
        }

        if (r.getExamType() == null || r.getExamType().isBlank()) {
            throw new IllegalArgumentException(
                    "Examination type is required");
        }

        // Must verify the academic year belongs to the current school
        AcademicYear academicYear = findYear(r.getAcademicYearId());

        String examinationName = r.getName().trim();

        // Prevent duplicate examination names within the same academic year
        boolean exists = examinationRepository
                .findByAcademicYear_IdAndNameIgnoreCase(
                        academicYear.getId(),
                        examinationName)
                .isPresent();

        if (exists) {
            throw new IllegalArgumentException(
                    "An examination with this name already exists "
                            + "in the selected academic year");
        }

        String status = r.getStatus() == null || r.getStatus().isBlank()
                ? "DRAFT"
                : r.getStatus().trim().toUpperCase(Locale.ROOT);

        Examination exam = Examination.builder()
                .academicYear(academicYear)
                .name(examinationName)
                .description(r.getDescription())
                .examType(r.getExamType().trim())
                .year(r.getYear())
                .status(status)
                .build();

        return examinationDto(examinationRepository.save(exam));
    }



    @Override
    @Transactional
    public ExaminationResponseDto updateExamination(
            Long id,
            ExaminationRequestDto r) {

        if (r == null) {
            throw new IllegalArgumentException(
                    "Examination request is required");
        }

        if (r.getAcademicYearId() == null) {
            throw new IllegalArgumentException(
                    "Academic year ID is required");
        }

        if (r.getName() == null || r.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Examination name is required");
        }

        if (r.getExamType() == null || r.getExamType().isBlank()) {
            throw new IllegalArgumentException(
                    "Examination type is required");
        }

        // Fetch only an examination belonging to the current school
        Examination exam = findExam(id);

        // Fetch only an academic year belonging to the current school
        AcademicYear academicYear = findYear(r.getAcademicYearId());

        String examinationName = r.getName().trim();

        // Prevent duplicate names within the selected academic year,
        // while allowing the current examination to retain its own name.
        examinationRepository
                .findByAcademicYear_IdAndNameIgnoreCase(
                        academicYear.getId(),
                        examinationName)
                .filter(existing -> !existing.getId().equals(exam.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "An examination with this name already exists "
                                    + "in the selected academic year");
                });

        exam.setAcademicYear(academicYear);
        exam.setName(examinationName);
        exam.setDescription(r.getDescription());
        exam.setExamType(r.getExamType().trim());
        exam.setYear(r.getYear());

        if (r.getStatus() != null && !r.getStatus().isBlank()) {
            exam.setStatus(r.getStatus().trim().toUpperCase(Locale.ROOT));
        }

        Examination saved = examinationRepository.save(exam);

        return examinationDto(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public List<ExaminationResponseDto> getExaminations(Long yearId) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        List<Examination> exams = yearId == null
                ? examinationRepository
                .findByAcademicYear_School_Id(schoolId)
                : examinationRepository
                .findByAcademicYear_IdAndAcademicYear_School_Id(
                        yearId,
                        schoolId
                );

        return exams.stream()
                .map(this::examinationDto)
                .toList();
    }


    private String getStringValue(Cell cell) {

        if (cell == null) {
            return null;
        }

        DataFormatter formatter =
                new DataFormatter();

        String value =
                formatter.formatCellValue(cell);

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private Long getLongValue(Cell cell) {

        String value = getStringValue(cell);

        if (value == null) {
            return null;
        }

        try {

            if (cell != null
                    && cell.getCellType()
                    == CellType.NUMERIC) {

                return cell.getNumericCellValue() % 1 == 0
                        ? (long) cell.getNumericCellValue()
                        : Long.parseLong(value);
            }

            return Long.parseLong(value);

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                    "Invalid numeric value: " + value
            );
        }
    }

    private Integer getIntegerValue(Cell cell) {

        String value = getStringValue(cell);

        if (value == null) {
            return null;
        }

        try {

            if (cell != null
                    && cell.getCellType()
                    == CellType.NUMERIC) {

                return (int) cell.getNumericCellValue();
            }

            return Integer.parseInt(value);

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                    "Invalid integer value: " + value
            );
        }
    }

    private boolean isRowEmpty(Row row) {

        if (row == null) {
            return true;
        }

        for (int i = 0; i < 7; i++) {

            Cell cell = row.getCell(i);

            if (cell != null
                    && cell.getCellType()
                    != CellType.BLANK
                    && getStringValue(cell) != null) {

                return false;
            }
        }

        return true;
    }

    private String getErrorMessage(Exception ex) {

        if (ex.getMessage() != null
                && !ex.getMessage().isBlank()) {

            return ex.getMessage();
        }

        if (ex.getCause() != null
                && ex.getCause().getMessage() != null) {

            return ex.getCause().getMessage();
        }

        return ex.getClass().getSimpleName();
    }

    @Override
    public ExamScheduleResponseDto createSchedule(
            ExamScheduleRequestDto r) {

        return scheduleDto(
                scheduleRepository.save(
                        buildSchedule(
                                new ExamSchedule(),
                                r
                        )
                )
        );
    }

    @Override
    public ExamScheduleResponseDto updateSchedule(
            Long id,
            ExamScheduleRequestDto r) {

        ExamSchedule existing = findSchedule(id);

// Only after this point, load existing results or modify the schedule.

        if (resultRepository
                .findByExamScheduleId(id)
                .size() > 0) {

            throw new IllegalStateException(
                    "A schedule with entered results cannot be changed"
            );
        }

        return scheduleDto(
                buildSchedule(
                        findSchedule(id),
                        r
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamScheduleResponseDto> getSchedules(
            Long examId,
            Long classId,
            Long sectionId,
            Long subjectId) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        List<ExamSchedule> schedules =
                scheduleRepository.findSchedulesForSchool(
                        schoolId,
                        examId,
                        classId,
                        sectionId,
                        subjectId);

        return schedules.stream()
                .map(this::scheduleDto)
                .toList();
    }


    @Override
    public ExamResultResponseDto enterMarks(
            ExamResultRequestDto r,
            String username,
            boolean admin) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        // 1. Validate request
        if (r.getMarksObtained() == null) {
            throw new IllegalArgumentException("Marks obtained is required");
        }

        if (r.getStudentId() == null) {
            throw new IllegalArgumentException("Student ID is required");
        }

        if (r.getExamScheduleId() == null) {
            throw new IllegalArgumentException("Exam schedule ID is required");
        }

        // 2. Fetch schedule belonging to the logged-in user's school
        ExamSchedule schedule = findSchedule(r.getExamScheduleId());

        // 3. Fetch student belonging to the same school
        Student student = studentRepository
                .findByIdAndSection_ClassEntity_School_Id(
                        r.getStudentId(),
                        schoolId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Student not found"));

        // 4. Validate student eligibility for the scheduled class
        if (student.getSection() == null
                || student.getSection().getClassEntity() == null
                || schedule.getClassEntity() == null
                || !student.getSection().getClassEntity().getId()
                .equals(schedule.getClassEntity().getId())) {
            throw new IllegalArgumentException(
                    "Student does not belong to the scheduled class");
        }

        // If the schedule targets a specific section, validate it too
        if (schedule.getSection() != null
                && !schedule.getSection().getId()
                .equals(student.getSection().getId())) {
            throw new IllegalArgumentException(
                    "Student does not belong to the scheduled section");
        }

        // 5. Validate marks against the schedule's configured maximum
        if (schedule.getMaxMarks() == null
                || schedule.getMaxMarks() <= 0) {
            throw new IllegalStateException(
                    "Maximum marks are not configured correctly for this schedule");
        }

        if (r.getMarksObtained() < 0) {
            throw new IllegalArgumentException(
                    "Marks obtained cannot be negative");
        }

        if (r.getMarksObtained() > schedule.getMaxMarks()) {
            throw new IllegalArgumentException(
                    "Marks obtained cannot exceed the schedule maximum marks");
        }

        // Keep any additional existing eligibility checks
        validateStudentScope(student, schedule);

        // 6. Resolve the teacher and validate access
        Teacher teacher = resolveMarkingTeacher(
                r.getTeacherId(),
                username,
                admin);

        if (!admin) {
            teacherAccessService.validateTeacherAccessToClassAndSubject(
                    teacher.getId(),
                    schedule.getClassEntity().getId(),
                    schedule.getSubject().getId());
        }

        // 7. Load an existing result only within this school,
        // or create a new result for this schedule and student.
        ExamResult result = resultRepository
                .findByScheduleStudentAndSchool(
                        schedule.getId(),
                        student.getId(),
                        schoolId)
                .orElseGet(() -> {
                    ExamResult newResult = new ExamResult();
                    newResult.setExamSchedule(schedule);
                    newResult.setStudent(student);
                    return newResult;
                });

        result.setExamSchedule(schedule);
        result.setStudent(student);
        result.setMarkedByTeacher(teacher);
        result.setMarksObtained(r.getMarksObtained());
        result.setMaxMarks(schedule.getMaxMarks());

        result.setPercentage(
                round(r.getMarksObtained() * 100.0 / schedule.getMaxMarks()));

        result.setGrade(gradeFor(result.getPercentage()));
        result.setRemarks(r.getRemarks());
        result.setStatus(
                r.getStatus() == null || r.getStatus().isBlank()
                        ? "ENTERED"
                        : r.getStatus());

        // Any marks update requires publication again
        result.setIsPublished(false);
        result.setPublishedAt(null);
        result.setMarkedAt(LocalDateTime.now());

        // 8. Save and recalculate ranks
        ExamResult saved = resultRepository.save(result);

        recalculateRanks(schedule.getId());

        // Re-fetch using the school-scoped repository method
        ExamResult savedResult = resultRepository
                .findByIdAndSchoolId(saved.getId(), schoolId)
                .orElseThrow(() ->
                        new IllegalStateException("Saved exam result not found"));

        return resultDto(savedResult);
    }


    @Override
    @Transactional(readOnly = true)
    public List<ExamResultResponseDto> getResults(
            Long scheduleId,
            Long studentId) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        if (scheduleId != null) {
            findSchedule(scheduleId);
        }

        List<ExamResult> results =
                scheduleId != null
                        ? resultRepository.findByScheduleAndSchool(scheduleId, schoolId)
                        : studentId != null
                        ? resultRepository.findByStudentAndSchool(studentId, schoolId)
                        : resultRepository.findAll();

        return results.stream()
                .map(this::resultDto)
                .toList();
    }

    @Override
    public ExamResultResponseDto setResultPublication(
            Long id,
            boolean published) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        ExamResult result = resultRepository
                .findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Result not found"));

        result.setIsPublished(published);
        result.setPublishedAt(
                published
                        ? LocalDateTime.now()
                        : null
        );
        result.setStatus(
                published
                        ? "PUBLISHED"
                        : "ENTERED"
        );

        return resultDto(result);
    }


    private ExamSchedule buildSchedule(
            ExamSchedule s,
            ExamScheduleRequestDto r) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        // 1. Validate required IDs
        if (r.getExaminationId() == null) {
            throw new IllegalArgumentException("Examination ID is required");
        }

        if (r.getClassId() == null) {
            throw new IllegalArgumentException("Class ID is required");
        }

        if (r.getSubjectId() == null) {
            throw new IllegalArgumentException("Subject ID is required");
        }

        if (r.getExamDate() == null) {
            throw new IllegalArgumentException("Exam date is required");
        }

        if (r.getStartTime() == null) {
            throw new IllegalArgumentException("Exam start time is required");
        }

        if (r.getDuration() == null || r.getDuration() <= 0) {
            throw new IllegalArgumentException(
                    "Exam duration must be greater than zero");
        }

        if (r.getMaxMarks() == null || r.getMaxMarks() <= 0) {
            throw new IllegalArgumentException(
                    "Maximum marks must be greater than zero");
        }

        // 2. Fetch examination within the authenticated school
        Examination exam = findExam(r.getExaminationId());

        if (!"PUBLISHED".equalsIgnoreCase(exam.getStatus())) {
            throw new IllegalArgumentException(
                    "Only published examinations can be scheduled");
        }

        // 3. Fetch class within the authenticated school
        ClassEntity clazz = classRepository
                .findByIdAndSchoolId(r.getClassId(), schoolId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Class not found in this school"));

        // 4. Fetch subject within the authenticated school
        Subject subject = subjectRepository
                .findByIdAndSchoolId(r.getSubjectId(), schoolId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Subject not found in this school"));

        // 5. Validate optional section
        Section section = null;

        if (r.getSectionId() != null) {
            section = sectionRepository
                    .findByIdWithClassAndSchool(r.getSectionId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Section not found"));

            if (section.getClassEntity() == null
                    || section.getClassEntity().getSchool() == null
                    || !section.getClassEntity().getId()
                    .equals(clazz.getId())
                    || !section.getClassEntity().getSchool().getId()
                    .equals(schoolId)) {

                throw new IllegalArgumentException(
                        "Section does not belong to the selected class "
                                + "in this school");
            }
        }

        // 6. Verify that the subject is assigned to the selected class
        boolean assignedToClass =
                classSubjectRepository
                        .existsByClassEntityIdAndSubjectId(
                                clazz.getId(),
                                subject.getId());

        if (!assignedToClass) {
            throw new IllegalArgumentException(
                    "Subject is not assigned to the selected class");
        }

        // 7. Validate optional teacher
        Teacher teacher = null;

        if (r.getSubjectTeacherId() != null) {
            teacher = teacherRepository
                    .findByIdAndSchoolId(
                            r.getSubjectTeacherId(),
                            schoolId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Teacher not found in this school"));

            teacherAccessService
                    .validateTeacherAccessToClassAndSubject(
                            teacher.getId(),
                            clazz.getId(),
                            subject.getId());
        }

        // 8. Build/update schedule
        s.setExamination(exam);
        s.setClassEntity(clazz);
        s.setSection(section);
        s.setSubject(subject);
        s.setSubjectTeacher(teacher);
        s.setExamDate(r.getExamDate());
        s.setStartTime(r.getStartTime());
        s.setDuration(r.getDuration());
        s.setMaxMarks(r.getMaxMarks());
        s.setExamType(r.getExamType());
        s.setRoomNumber(r.getRoomNumber());

        s.setStatus(
                r.getStatus() == null || r.getStatus().isBlank()
                        ? "SCHEDULED"
                        : r.getStatus().trim());

        return s;
    }


    private Teacher resolveMarkingTeacher(
            Long requested,
            String username,
            boolean admin) {

        if (admin) {

            if (requested == null) {
                throw new IllegalArgumentException(
                        "Teacher ID is required for administrator entry"
                );
            }

            return teacherRepository.findById(requested)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Teacher not found: "
                                            + requested
                            )
                    );
        }

        Long userId =
                userRepository.findByUsername(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Authenticated user not found"
                                )
                        )
                        .getId();

        return teacherRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Teacher profile not found for authenticated user"
                        )
                );
    }

    private void validateStudentScope(
            Student student,
            ExamSchedule schedule) {

        if (student.getSection() == null
                || !student.getSection()
                .getClassEntity()
                .getId()
                .equals(schedule.getClassEntity().getId())
                || (schedule.getSection() != null
                && !student.getSection()
                .getId()
                .equals(schedule.getSection().getId()))) {

            throw new IllegalArgumentException(
                    "Student is not assigned to this exam schedule's class or section"
            );
        }
    }


    private void recalculateRanks(Long scheduleId) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        // Validate that the schedule belongs to the authenticated school
        ExamSchedule schedule = findSchedule(scheduleId);

        List<ExamResult> results = resultRepository
                .findByScheduleAndSchool(schedule.getId(), schoolId)
                .stream()
                .filter(r -> r.getMarksObtained() != null)
                .filter(r -> r.getPercentage() != null)
                .toList();

        // Overall class rank
        List<ExamResult> classOrder = results.stream()
                .sorted(
                        Comparator.comparing(
                                ExamResult::getPercentage,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                )
                .toList();

        rank(classOrder, false);

        // Section-wise rank
        results.stream()
                .filter(r -> r.getStudent() != null)
                .filter(r -> r.getStudent().getSection() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getStudent().getSection().getId()
                ))
                .values()
                .forEach(group -> {
                    List<ExamResult> sectionOrder = group.stream()
                            .sorted(
                                    Comparator.comparing(
                                            ExamResult::getPercentage,
                                            Comparator.nullsLast(
                                                    Comparator.reverseOrder()
                                            )
                                    )
                            )
                            .toList();

                    rank(sectionOrder, true);
                });

        // Persist recalculated ranks
        resultRepository.saveAll(results);
    }

    private void rank(
            List<ExamResult> ordered,
            boolean section) {

        double previous = Double.NaN;
        int rank = 0;

        for (int i = 0; i < ordered.size(); i++) {

            ExamResult r = ordered.get(i);

            if (Double.compare(
                    r.getPercentage(),
                    previous
            ) != 0) {

                rank = i + 1;
                previous = r.getPercentage();
            }

            if (section) {
                r.setSectionRank(rank);
            } else {
                r.setClassRank(rank);
            }
        }
    }

    private String gradeFor(Double percentage) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();
        return gradeRuleRepository
                .findAllBySchool_IdOrderByMinimumPercentageDesc(schoolId)
                .stream()
                .filter(g ->
                        percentage >= g.getMinimumPercentage()
                                && percentage <= g.getMaximumPercentage()
                )
                .findFirst()
                .map(GradeRule::getGrade)
                .orElse("UNDEFINED");
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private AcademicYear findYear(Long id) {

        if (id == null) {
            return null;
        }

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return academicYearRepository
                .findByIdAndSchool_Id(id, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Academic year not found for current school: " + id
                        )
                );
    }

    private Examination findExam(Long id) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return examinationRepository
                .findByIdAndAcademicYear_School_Id(id, schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Examination not found for current school: " + id
                        )
                );
    }

    private ExamSchedule findSchedule(Long id) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return scheduleRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Exam schedule not found"));
    }

    private ExaminationResponseDto examinationDto(
            Examination e) {

        return ExaminationResponseDto.builder()
                .id(e.getId())
                .academicYearId(
                        e.getAcademicYear() == null
                                ? null
                                : e.getAcademicYear().getId()
                )
                .academicYearName(
                        e.getAcademicYear() == null
                                ? null
                                : e.getAcademicYear().getName()
                )
                .name(e.getName())
                .description(e.getDescription())
                .examType(e.getExamType())
                .year(e.getYear())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private ExamScheduleResponseDto scheduleDto(
            ExamSchedule e) {

        Subject subject = e.getSubject();

        return ExamScheduleResponseDto.builder()
                .id(e.getId())
                .examinationId(
                        e.getExamination().getId()
                )
                .examinationName(
                        e.getExamination().getName()
                )
                .classId(
                        e.getClassEntity().getId()
                )
                .className(
                        e.getClassEntity().getName()
                )
                .sectionId(
                        e.getSection() == null
                                ? null
                                : e.getSection().getId()
                )
                .sectionName(
                        e.getSection() == null
                                ? null
                                : e.getSection().getName()
                )
                .subjectId(
                        subject == null
                                ? null
                                : subject.getId()
                )
                .subjectName(
                        subject == null
                                ? null
                                : subject.getName()
                )
                .subjectCode(
                        subject == null
                                ? null
                                : subject.getCode()
                )
                .subjectTeacherId(
                        e.getSubjectTeacher() == null
                                ? null
                                : e.getSubjectTeacher().getId()
                )
                .subjectTeacherName(
                        e.getSubjectTeacher() == null
                                ? null
                                : e.getSubjectTeacher().getName()
                )
                .examDate(e.getExamDate())
                .startTime(e.getStartTime())
                .duration(e.getDuration())
                .maxMarks(e.getMaxMarks())
                .examType(e.getExamType())
                .roomNumber(e.getRoomNumber())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private ExamResultResponseDto resultDto(
            ExamResult e) {

        return ExamResultResponseDto.builder()
                .id(e.getId())
                .examScheduleId(
                        e.getExamSchedule().getId()
                )
                .studentId(
                        e.getStudent().getId()
                )
                .studentName(
                        e.getStudent().getName()
                )
                .studentRollNumber(
                        e.getStudent().getRollNumber()
                )
                .markedByTeacherId(
                        e.getMarkedByTeacher() == null
                                ? null
                                : e.getMarkedByTeacher().getId()
                )
                .markedByTeacherName(
                        e.getMarkedByTeacher() == null
                                ? null
                                : e.getMarkedByTeacher().getName()
                )
                .marksObtained(e.getMarksObtained())
                .maxMarks(e.getMaxMarks())
                .percentage(e.getPercentage())
                .grade(e.getGrade())
                .classRank(e.getClassRank())
                .sectionRank(e.getSectionRank())
                .remarks(e.getRemarks())
                .status(e.getStatus())
                .isPublished(e.getIsPublished())
                .markedAt(e.getMarkedAt())
                .publishedAt(e.getPublishedAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }







    @Override
    @Transactional
    public GradeRuleImportResponseDto importGradeRulesExcel(MultipartFile file) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        List<GradeRuleImportErrorDto> errors = new ArrayList<>();
        int totalRows = 0;
        int created = 0;
        int updated = 0;

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is empty");
        }

        String filename = file.getOriginalFilename();

        if (filename == null
                || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException(
                    "Only .xlsx Excel files are supported");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel sheet is missing");
            }

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalArgumentException(
                        "Excel header row is missing");
            }

            // Expected headers: grade, minPercentage, maxPercentage
            validateGradeRuleHeader(headerRow);

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (isRowEmpty(row)) {
                    continue;
                }

                totalRows++;
                int excelRowNumber = rowIndex + 1;

                try {
                    // Read columns according to the Excel template.
                    String grade = getStringValue(row.getCell(0));
                    Double minimumPercentage =
                            getDoubleValue(row.getCell(1));
                    Double maximumPercentage =
                            getDoubleValue(row.getCell(2));

                    // Validate grade.
                    if (grade == null || grade.isBlank()) {
                        throw new IllegalArgumentException(
                                "Grade is required");
                    }

                    grade = grade.trim();

                    // Validate minimum percentage.
                    if (minimumPercentage == null) {
                        throw new IllegalArgumentException(
                                "Minimum Percentage is required");
                    }

                    if (!Double.isFinite(minimumPercentage)
                            || minimumPercentage < 0
                            || minimumPercentage > 100) {
                        throw new IllegalArgumentException(
                                "Minimum Percentage must be between 0 and 100");
                    }

                    // Validate maximum percentage.
                    if (maximumPercentage == null) {
                        throw new IllegalArgumentException(
                                "Maximum Percentage is required");
                    }

                    if (!Double.isFinite(maximumPercentage)
                            || maximumPercentage < 0
                            || maximumPercentage > 100) {
                        throw new IllegalArgumentException(
                                "Maximum Percentage must be between 0 and 100");
                    }

                    if (minimumPercentage > maximumPercentage) {
                        throw new IllegalArgumentException(
                                "Minimum Percentage cannot exceed Maximum Percentage");
                    }

                    /*
                     * Find the existing grade only within the current school.
                     * If found, update it; otherwise create a new rule.
                     */
                    GradeRule gradeRule = gradeRuleRepository
                            .findBySchool_IdAndGradeIgnoreCase(
                                    schoolId, grade)
                            .orElse(null);

                    boolean isNew = (gradeRule == null);

                    if (isNew) {
                        gradeRule = GradeRule.builder()
                                .school(currentSchoolService.getCurrentSchool())
                                .build();
                    }

                    /*
                     * Validate percentage overlap.
                     * Exclude the current rule when updating.
                     */
                    validateGradeRuleOverlap(
                            schoolId,
                            minimumPercentage,
                            maximumPercentage,
                            gradeRule.getId()
                    );

                    // Set the values from Excel.
                    gradeRule.setGrade(grade);
                    gradeRule.setMinimumPercentage(minimumPercentage);
                    gradeRule.setMaximumPercentage(maximumPercentage);

                    gradeRuleRepository.save(gradeRule);

                    if (isNew) {
                        created++;
                    } else {
                        updated++;
                    }

                } catch (Exception e) {
                    errors.add(
                            GradeRuleImportErrorDto.builder()
                                    .row(excelRowNumber)
                                    .message(getErrorMessage(e))
                                    .build()
                    );
                }
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to read Excel file: " + getErrorMessage(e), e);
        }

        return GradeRuleImportResponseDto.builder()
                .totalRows(totalRows)
                .created(created)
                .updated(updated)
                .errors(errors.size())
                .errorDetails(errors)
                .build();
    }

    private void validateGradeRuleHeader(Row headerRow) {

        String[] expectedHeaders = {
                "Grade",
                "Minimum Percentage",
                "Maximum Percentage"
        };

        String[] acceptedHeaders = {
                "grade",
                "minpercentage",
                "maxpercentage"
        };

        for (int i = 0; i < expectedHeaders.length; i++) {

            Cell cell = headerRow.getCell(i);

            String actual = cell == null
                    ? ""
                    : getStringValue(cell).trim();

            boolean valid;

            if (i == 0) {
                valid = actual.equalsIgnoreCase("Grade");
            } else if (i == 1) {
                valid = actual.equalsIgnoreCase("Minimum Percentage")
                        || actual.equalsIgnoreCase("minPercentage");
            } else {
                valid = actual.equalsIgnoreCase("Maximum Percentage")
                        || actual.equalsIgnoreCase("maxPercentage");
            }

            if (!valid) {
                throw new IllegalArgumentException(
                        "Invalid Excel header at column " + (i + 1)
                                + ". Expected: " + expectedHeaders[i]
                                + " or " + acceptedHeaders[i]
                                + ", Found: " + actual
                );
            }
        }
    }

    private void validateGradeRuleOverlap(
            Long schoolId,
            Double minimumPercentage,
            Double maximumPercentage,
            Long currentId) {

        boolean overlaps = gradeRuleRepository
                .findAllBySchool_Id(schoolId)
                .stream()
                .anyMatch(existing -> {

                    if (currentId != null
                            && existing.getId().equals(currentId)) {
                        return false;
                    }

                    return minimumPercentage
                            <= existing.getMaximumPercentage()
                            && maximumPercentage
                            >= existing.getMinimumPercentage();
                });

        if (overlaps) {
            throw new IllegalArgumentException(
                    "Grade percentage range overlaps an existing rule"
            );
        }
    }

    private Double getDoubleValue(Cell cell) {

        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {

            case NUMERIC:
                return cell.getNumericCellValue();

            case STRING:
                String value = cell.getStringCellValue();

                if (value == null || value.isBlank()) {
                    return null;
                }

                try {
                    return Double.parseDouble(value.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Invalid number value: " + value
                    );
                }

            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Invalid formula value"
                    );
                }

            default:
                return null;
        }
    }




    private LocalDate getLocalDateValue(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {

            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }

            throw new IllegalArgumentException(
                    "Invalid date value: " + cell.getNumericCellValue()
            );
        }

        if (cell.getCellType() == CellType.STRING) {

            String value = cell.getStringCellValue();

            if (value == null || value.isBlank()) {
                return null;
            }

            try {
                return LocalDate.parse(value.trim());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "Invalid date format: " + value
                                + ". Expected yyyy-MM-dd"
                );
            }
        }

        if (cell.getCellType() == CellType.FORMULA) {

            if (cell.getCachedFormulaResultType() == CellType.NUMERIC
                    && DateUtil.isCellDateFormatted(cell)) {

                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
        }

        return null;
    }


    private LocalTime getLocalTimeValue(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {

            double numericValue = cell.getNumericCellValue();

            return LocalTime.ofSecondOfDay(
                    Math.round(numericValue * 24 * 60 * 60)
            );
        }

        if (cell.getCellType() == CellType.STRING) {

            String value = cell.getStringCellValue();

            if (value == null || value.isBlank()) {
                return null;
            }

            value = value.trim();

            try {
                return LocalTime.parse(value);
            } catch (DateTimeParseException e) {

                try {
                    return LocalTime.parse(
                            value,
                            DateTimeFormatter.ofPattern("HH:mm")
                    );
                } catch (DateTimeParseException ex) {

                    throw new IllegalArgumentException(
                            "Invalid time format: " + value
                                    + ". Expected HH:mm or HH:mm:ss"
                    );
                }
            }
        }

        if (cell.getCellType() == CellType.FORMULA) {

            if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {

                double numericValue = cell.getNumericCellValue();

                return LocalTime.ofSecondOfDay(
                        Math.round(numericValue * 24 * 60 * 60)
                );
            }
        }

        return null;
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ExamScheduleResponseDto> getExamScheduleReport(
            LocalDate from,
            LocalDate to,
            Pageable pageable) {

        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required");
        }

        if (to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "End date must not precede start date");
        }

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return scheduleRepository
                .findReportBySchoolAndExamDateBetween(
                        schoolId,
                        from,
                        to,
                        pageable)
                .map(this::scheduleDto);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ExamResultResponseDto> getAcademicPerformanceReport(
            Pageable pageable) {
        Long schoolId = currentSchoolService.getCurrentSchoolId();
        return resultRepository
                .findAllBySchool(schoolId, pageable)
                .map(this::resultDto);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ExamResultResponseDto> getStudentPerformanceReport(
            Long studentId,
            Pageable pageable,
            boolean staff) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        // Verify that the student belongs to the authenticated school
        studentRepository
                .findByIdAndSection_ClassEntity_School_Id(
                        studentId,
                        schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Student not found in this school: " + studentId));

        // Fetch only this school's results.
        // Staff can see all results; non-staff can see published results only.
        Page<ExamResult> resultPage = staff
                ? resultRepository.findStudentReportBySchool(
                studentId,
                schoolId,
                pageable)
                : resultRepository.findPublishedByStudentAndSchool(
                studentId,
                schoolId,
                pageable);

        return resultPage.map(this::resultDto);
    }



    @Override
    @Transactional(readOnly = true)
    public Page<ExamScheduleResponseDto> getTeacherClassPerformanceReport(
            Long classId,
            Pageable pageable) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        // Verify that the class belongs to the logged-in user's school
        classRepository.findByIdAndSchoolId(classId, schoolId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Class not found in this school"));

        // Fetch only schedules belonging to this school and class
        return scheduleRepository
                .findReportBySchoolAndClassId(
                        schoolId,
                        classId,
                        pageable)
                .map(this::scheduleDto);
    }




    @Override
    public List<ExamResultResponseDto> setStudentResultsPublication(
            Long studentId,
            Long examinationId,
            boolean published) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        Student student = studentRepository
                .findByIdAndSection_ClassEntity_School_Id(studentId, schoolId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Student not found"));

        Examination examination = findExam(examinationId);

        List<ExamResult> results =
                resultRepository.findByStudentAndSchool(studentId, schoolId)
                        .stream()
                        .filter(r -> r.getExamSchedule()
                                .getExamination().getId()
                                .equals(examination.getId()))
                        .toList();

        LocalDateTime now = LocalDateTime.now();

        for (ExamResult result : results) {
            result.setIsPublished(published);
            result.setPublishedAt(
                    published ? now : null
            );
            result.setStatus(
                    published ? "PUBLISHED" : "ENTERED"
            );
        }

        return results.stream()
                .map(this::resultDto)
                .toList();
    }


    @Override
    @Transactional
    public List<ExamResultResponseDto> setExaminationResultsPublication(
            Long examinationId,
            boolean published) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        // Only an examination belonging to this school is accepted
        Examination examination = findExam(examinationId);

        // These schedules belong to the already validated examination
        List<ExamSchedule> schedules =
                scheduleRepository.findByExaminationId(examination.getId());

        List<ExamResult> results = new ArrayList<>();

        for (ExamSchedule schedule : schedules) {

            // Never load results globally across all schools
            List<ExamResult> scheduleResults =
                    resultRepository.findByScheduleAndSchool(
                            schedule.getId(),
                            schoolId
                    );

            results.addAll(scheduleResults);
        }

        LocalDateTime now = LocalDateTime.now();

        for (ExamResult result : results) {
            result.setIsPublished(published);
            result.setPublishedAt(published ? now : null);
            result.setStatus(published ? "PUBLISHED" : "ENTERED");
        }

        // Persist publication changes
        List<ExamResult> savedResults = resultRepository.saveAll(results);

        return savedResults.stream()
                .map(this::resultDto)
                .toList();
    }



}

