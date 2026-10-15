
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
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
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

    @Override
    public ExaminationResponseDto createExamination(ExaminationRequestDto r) {
        Examination exam = Examination.builder()
                .academicYear(findYear(r.getAcademicYearId()))
                .name(r.getName())
                .description(r.getDescription())
                .examType(r.getExamType())
                .year(r.getYear())
                .status(r.getStatus())
                .build();

        return examinationDto(examinationRepository.save(exam));
    }

    @Override
    public ExaminationResponseDto updateExamination(
            Long id,
            ExaminationRequestDto r) {

        Examination exam = findExam(id);

        exam.setAcademicYear(findYear(r.getAcademicYearId()));
        exam.setName(r.getName());
        exam.setDescription(r.getDescription());
        exam.setExamType(r.getExamType());
        exam.setYear(r.getYear());
        exam.setStatus(r.getStatus());

        return examinationDto(exam);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExaminationResponseDto> getExaminations(Long yearId) {

        List<Examination> exams =
                yearId == null
                        ? examinationRepository.findAll()
                        : examinationRepository.findByAcademicYearId(yearId);

        return exams.stream()
                .map(this::examinationDto)
                .toList();
    }

    /*
     * ============================================================
     * EXCEL IMPORT - EXAMINATIONS
     * ============================================================
     */

//    @Override
//    public ExaminationImportResponseDto importExaminationsExcel(
//            MultipartFile file) {
//
//        if (file == null || file.isEmpty()) {
//            throw new IllegalArgumentException("Excel file is required");
//        }
//
//        String fileName = file.getOriginalFilename();
//
//        if (fileName == null
//                || !fileName.toLowerCase().endsWith(".xlsx")) {
//
//            throw new IllegalArgumentException(
//                    "Only .xlsx Excel files are supported"
//            );
//        }
//
//        int totalRows = 0;
//        int created = 0;
//        int updated = 0;
//
//        List<ExaminationImportErrorDto> errors =
//                new ArrayList<>();
//
//        try (InputStream inputStream = file.getInputStream();
//             Workbook workbook = new XSSFWorkbook(inputStream)) {
//
//            if (workbook.getNumberOfSheets() == 0) {
//                throw new IllegalArgumentException(
//                        "Excel file does not contain any sheet"
//                );
//            }
//
//            Sheet sheet = workbook.getSheetAt(0);
//
//            if (sheet.getLastRowNum() < 1) {
//                throw new IllegalArgumentException(
//                        "Excel file does not contain any examination data"
//                );
//            }
//
//            Row headerRow = sheet.getRow(0);
//
//            validateExaminationHeader(headerRow);
//
//            for (int rowIndex = 1;
//                 rowIndex <= sheet.getLastRowNum();
//                 rowIndex++) {
//
//                Row row = sheet.getRow(rowIndex);
//
//                if (isRowEmpty(row)) {
//                    continue;
//                }
//
//                totalRows++;
//
//                int excelRowNumber = rowIndex + 1;
//
//                try {
//
//                    Long id = getLongValue(row.getCell(0));
//
//                    Long academicYearId =
//                            getLongValue(row.getCell(1));
//
//                    String name =
//                            getStringValue(row.getCell(2));
//
//                    String description =
//                            getStringValue(row.getCell(3));
//
//                    String examType =
//                            getStringValue(row.getCell(4));
//
//                    Integer year =
//                            getIntegerValue(row.getCell(5));
//
//                    String status =
//                            getStringValue(row.getCell(6));
//
//                    /*
//                     * Required validations
//                     */
//
//                    if (name == null || name.isBlank()) {
//                        throw new IllegalArgumentException(
//                                "Name is required"
//                        );
//                    }
//
//                    if (examType == null || examType.isBlank()) {
//                        throw new IllegalArgumentException(
//                                "Exam type is required"
//                        );
//                    }
//
//                    /*
//                     * Build request DTO.
//                     *
//                     * We intentionally reuse the existing
//                     * create/update logic so manual and Excel
//                     * operations behave consistently.
//                     */
//
//                    ExaminationRequestDto request =
//                            ExaminationRequestDto.builder()
//                                    .academicYearId(academicYearId)
//                                    .name(name)
//                                    .description(description)
//                                    .examType(examType)
//                                    .year(year)
//                                    .status(status)
//                                    .build();
//
//                    if (id == null) {
//
//                        /*
//                         * CREATE
//                         */
//
//                        Examination exam =
//                                Examination.builder()
//                                        .academicYear(
//                                                findYear(
//                                                        request.getAcademicYearId()
//                                                )
//                                        )
//                                        .name(request.getName())
//                                        .description(
//                                                request.getDescription()
//                                        )
//                                        .examType(
//                                                request.getExamType()
//                                        )
//                                        .year(request.getYear())
//                                        .status(request.getStatus())
//                                        .build();
//
//                        examinationRepository.save(exam);
//
//                        created++;
//
//                    } else {
//
//                        /*
//                         * UPDATE
//                         */
//
//                        Examination exam = findExam(id);
//
//                        exam.setAcademicYear(
//                                findYear(
//                                        request.getAcademicYearId()
//                                )
//                        );
//
//                        exam.setName(request.getName());
//                        exam.setDescription(
//                                request.getDescription()
//                        );
//                        exam.setExamType(
//                                request.getExamType()
//                        );
//                        exam.setYear(request.getYear());
//                        exam.setStatus(request.getStatus());
//
//                        examinationRepository.save(exam);
//
//                        updated++;
//                    }
//
//                } catch (Exception ex) {
//
//                    errors.add(
//                            ExaminationImportErrorDto.builder()
//                                    .row(excelRowNumber)
//                                    .message(
//                                            getErrorMessage(ex)
//                                    )
//                                    .build()
//                    );
//                }
//            }
//
//        } catch (Exception ex) {
//
//            if (ex instanceof IllegalArgumentException) {
//                throw (IllegalArgumentException) ex;
//            }
//
//            throw new IllegalArgumentException(
//                    "Failed to read examination Excel file: "
//                            + getErrorMessage(ex),
//                    ex
//            );
//        }
//
//        return ExaminationImportResponseDto.builder()
//                .totalRows(totalRows)
//                .created(created)
//                .updated(updated)
//                .errors(errors.size())
//                .errorDetails(errors)
//                .build();
//    }

//    private void validateExaminationHeader(Row headerRow) {
//
//        if (headerRow == null) {
//            throw new IllegalArgumentException(
//                    "Excel header row is missing"
//            );
//        }
//
//        String[] expectedHeaders = {
//                "ID",
//                "Academic Year ID",
//                "Name",
//                "Description",
//                "Exam Type",
//                "Year",
//                "Status"
//        };
//
//        for (int i = 0;
//             i < expectedHeaders.length;
//             i++) {
//
//            String actual =
//                    getStringValue(headerRow.getCell(i));
//
//            if (!expectedHeaders[i].equalsIgnoreCase(
//                    actual == null ? "" : actual.trim())) {
//
//                throw new IllegalArgumentException(
//                        "Invalid Excel header at column "
//                                + (i + 1)
//                                + ". Expected: "
//                                + expectedHeaders[i]
//                );
//            }
//        }
//    }

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

        List<ExamSchedule> schedules =
                scheduleRepository.findAll()
                        .stream()
                        .filter(s ->
                                examId == null
                                        || s.getExamination()
                                        .getId()
                                        .equals(examId))
                        .filter(s ->
                                classId == null
                                        || s.getClassEntity()
                                        .getId()
                                        .equals(classId))
                        .filter(s ->
                                sectionId == null
                                        || (s.getSection() != null
                                        && s.getSection()
                                        .getId()
                                        .equals(sectionId)))
                        .filter(s ->
                                subjectId == null
                                        || s.getSubject()
                                        .getId()
                                        .equals(subjectId))
                        .toList();

        return schedules.stream()
                .map(this::scheduleDto)
                .toList();
    }

    @Override
    public ExamResultResponseDto enterMarks(
            ExamResultRequestDto r,
            String username,
            boolean admin) {

        ExamSchedule schedule =
                findSchedule(r.getExamScheduleId());

        if (r.getMarksObtained() == null) {
            throw new IllegalArgumentException(
                    "Marks obtained is required"
            );
        }

        if (schedule.getMaxMarks() == null) {
            throw new IllegalStateException(
                    "Maximum marks are not configured for this schedule"
            );
        }

        if (r.getMarksObtained() < 0) {
            throw new IllegalArgumentException(
                    "Marks obtained cannot be negative"
            );
        }

        if (r.getMarksObtained() > schedule.getMaxMarks()) {
            throw new IllegalArgumentException(
                    "Marks obtained cannot exceed the schedule maximum marks"
            );
        }

        Student student =
                studentRepository.findById(r.getStudentId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Student not found: "
                                                + r.getStudentId()
                                )
                        );

        validateStudentScope(student, schedule);

        Teacher teacher =
                resolveMarkingTeacher(
                        r.getTeacherId(),
                        username,
                        admin
                );

        if (!admin) {

            teacherAccessService
                    .validateTeacherAccessToClassAndSubject(
                            teacher.getId(),
                            schedule.getClassEntity().getId(),
                            schedule.getSubject().getId()
                    );
        }

        ExamResult result =
                resultRepository
                        .findByExamScheduleIdAndStudentId(
                                schedule.getId(),
                                student.getId()
                        )
                        .orElseGet(ExamResult::new);

        result.setExamSchedule(schedule);
        result.setStudent(student);
        result.setMarkedByTeacher(teacher);

        result.setMarksObtained(
                r.getMarksObtained()
        );

        result.setMaxMarks(
                schedule.getMaxMarks()
        );

        result.setPercentage(
                round(
                        r.getMarksObtained()
                                * 100.0
                                / schedule.getMaxMarks()
                )
        );

        result.setGrade(
                gradeFor(result.getPercentage())
        );

        result.setRemarks(
                r.getRemarks()
        );

        result.setStatus(
                r.getStatus() == null
                        ? "ENTERED"
                        : r.getStatus()
        );

        result.setIsPublished(false);
        result.setPublishedAt(null);
        result.setMarkedAt(LocalDateTime.now());

        ExamResult saved =
                resultRepository.save(result);

        recalculateRanks(schedule.getId());

        return resultDto(
                resultRepository
                        .findById(saved.getId())
                        .orElseThrow()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamResultResponseDto> getResults(
            Long scheduleId,
            Long studentId) {

        List<ExamResult> results =
                scheduleId != null
                        ? resultRepository
                        .findByExamScheduleId(scheduleId)
                        : studentId != null
                        ? resultRepository
                        .findByStudentId(studentId)
                        : resultRepository.findAll();

        return results.stream()
                .map(this::resultDto)
                .toList();
    }

    @Override
    public ExamResultResponseDto setResultPublication(
            Long id,
            boolean published) {

        ExamResult result =
                resultRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Exam result not found: "
                                                + id
                                )
                        );

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

        if (r.getExaminationId() == null) {
            throw new IllegalArgumentException(
                    "Examination ID is required"
            );
        }
        Examination exam =
                findExam(r.getExaminationId());
        if (!"PUBLISHED".equalsIgnoreCase(exam.getStatus())) {
            throw new IllegalArgumentException(
                    "Only published examinations can be scheduled"
            );
        }

        if (r.getClassId() == null) {
            throw new IllegalArgumentException(
                    "Class ID is required"
            );
        }

        ClassEntity clazz =
                classRepository.findById(r.getClassId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Class not found: "
                                                + r.getClassId()
                                )
                        );

        Section section = null;

        if (r.getSectionId() != null) {

            section =
                    sectionRepository.findById(
                            r.getSectionId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Section not found: "
                                            + r.getSectionId()
                            )
                    );

            if (section.getClassEntity() == null
                    || !section.getClassEntity()
                    .getId()
                    .equals(clazz.getId())) {

                throw new IllegalArgumentException(
                        "Section does not belong to the selected class"
                );
            }
        }

        Subject subject =
                subjectRepository.findById(r.getSubjectId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Subject not found: "
                                                + r.getSubjectId()
                                )
                        );

        if (subject.getSchool() == null
                || clazz.getSchool() == null
                || !subject.getSchool()
                .getId()
                .equals(clazz.getSchool().getId())) {

            throw new IllegalArgumentException(
                    "Subject does not belong to the same school as the class"
            );
        }

        boolean assignedToClass =
                classSubjectRepository
                        .existsByClassEntityIdAndSubjectId(
                                clazz.getId(),
                                subject.getId()
                        );

        if (!assignedToClass) {

            throw new IllegalArgumentException(
                    "Subject is not assigned to the selected class"
            );
        }

        Teacher teacher = null;

        if (r.getSubjectTeacherId() != null) {

            teacher =
                    teacherRepository.findById(
                            r.getSubjectTeacherId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Teacher not found: "
                                            + r.getSubjectTeacherId()
                            )
                    );

            teacherAccessService
                    .validateTeacherAccessToClassAndSubject(
                            teacher.getId(),
                            clazz.getId(),
                            subject.getId()
                    );
        }

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
                r.getStatus() == null
                        ? "SCHEDULED"
                        : r.getStatus()
        );

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

        List<ExamResult> results =
                resultRepository
                        .findByExamScheduleId(scheduleId)
                        .stream()
                        .filter(r -> r.getMarksObtained() != null)
                        .toList();

        List<ExamResult> classOrder =
                results.stream()
                        .sorted(
                                Comparator.comparing(
                                        ExamResult::getPercentage
                                ).reversed()
                        )
                        .toList();

        rank(classOrder, false);

        results.stream()
                .filter(r ->
                        r.getStudent().getSection() != null)
                .collect(
                        Collectors.groupingBy(
                                r -> r.getStudent()
                                        .getSection()
                                        .getId()
                        )
                )
                .values()
                .forEach(group ->
                        rank(
                                group.stream()
                                        .sorted(
                                                Comparator.comparing(
                                                        ExamResult::getPercentage
                                                ).reversed()
                                        )
                                        .toList(),
                                true
                        )
                );
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

        return gradeRuleRepository
                .findAllByOrderByMinimumPercentageDesc()
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

        return id == null
                ? null
                : academicYearRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Academic year not found: "
                                        + id
                        )
                );
    }

    private Examination findExam(Long id) {

        return examinationRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Examination not found: "
                                        + id
                        )
                );
    }

    private ExamSchedule findSchedule(Long id) {

        return scheduleRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Exam schedule not found: "
                                        + id
                        )
                );
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


//    @Override
//    @Transactional
//    public ExamResultImportResponseDto importExamResultsExcel(
//            MultipartFile file) {
//
//        List<ExamResultImportErrorDto> errors = new ArrayList<>();
//
//        int totalRows = 0;
//        int created = 0;
//        int updated = 0;
//
//        if (file == null || file.isEmpty()) {
//            throw new IllegalArgumentException("Excel file is empty");
//        }
//
//        String filename = file.getOriginalFilename();
//
//        if (filename == null
//                || !filename.toLowerCase().endsWith(".xlsx")) {
//            throw new IllegalArgumentException(
//                    "Only .xlsx Excel files are supported"
//            );
//        }
//
//        try (InputStream inputStream = file.getInputStream();
//             Workbook workbook = new XSSFWorkbook(inputStream)) {
//
//            Sheet sheet = workbook.getSheetAt(0);
//
//            if (sheet == null) {
//                throw new IllegalArgumentException(
//                        "Excel sheet is missing"
//                );
//            }
//
//            Row headerRow = sheet.getRow(0);
//
//            if (headerRow == null) {
//                throw new IllegalArgumentException(
//                        "Excel header row is missing"
//                );
//            }
//
//            validateExamResultHeader(headerRow);
//
//            for (int rowIndex = 1;
//                 rowIndex <= sheet.getLastRowNum();
//                 rowIndex++) {
//
//                Row row = sheet.getRow(rowIndex);
//
//                if (isRowEmpty(row)) {
//                    continue;
//                }
//
//                totalRows++;
//
//                int excelRowNumber = rowIndex + 1;
//
//                try {
//
//                    Long id = getLongValue(
//                            row.getCell(0)
//                    );
//
//                    Long examScheduleId = getLongValue(
//                            row.getCell(1)
//                    );
//
//                    Long studentId = getLongValue(
//                            row.getCell(2)
//                    );
//
//                    Long teacherId = getLongValue(
//                            row.getCell(3)
//                    );
//
//                    Integer marksObtained = getIntegerValue(
//                            row.getCell(4)
//                    );
//
//                    String remarks = getStringValue(
//                            row.getCell(5)
//                    );
//
//                    String status = getStringValue(
//                            row.getCell(6)
//                    );
//
//                    if (examScheduleId == null) {
//                        throw new IllegalArgumentException(
//                                "Exam Schedule ID is required"
//                        );
//                    }
//
//                    if (studentId == null) {
//                        throw new IllegalArgumentException(
//                                "Student ID is required"
//                        );
//                    }
//
//                    if (marksObtained == null) {
//                        throw new IllegalArgumentException(
//                                "Marks Obtained is required"
//                        );
//                    }
//
//                    if (marksObtained < 0) {
//                        throw new IllegalArgumentException(
//                                "Marks cannot be negative"
//                        );
//                    }
//
//                    /*
//                     * Reuse existing enterMarks() logic.
//                     *
//                     * Excel import is performed by SCHOOL_ADMIN,
//                     * therefore admin = true.
//                     */
//                    ExamResultRequestDto request =
//                            ExamResultRequestDto.builder()
//                                    .examScheduleId(examScheduleId)
//                                    .studentId(studentId)
//                                    .teacherId(teacherId)
//                                    .marksObtained(marksObtained)
//                                    .remarks(remarks)
//                                    .status(status)
//                                    .build();
//
//                    /*
//                     * Check whether result already exists before
//                     * calling enterMarks(), because enterMarks()
//                     * performs an upsert.
//                     */
//                    boolean alreadyExists =
//                            resultRepository
//                                    .findByExamScheduleIdAndStudentId(
//                                            examScheduleId,
//                                            studentId
//                                    )
//                                    .isPresent();
//
//                    enterMarks(
//                            request,
//                            null,
//                            true
//                    );
//
//                    if (alreadyExists) {
//                        updated++;
//                    } else {
//                        created++;
//                    }
//
//                } catch (Exception e) {
//
//                    errors.add(
//                            ExamResultImportErrorDto.builder()
//                                    .row(excelRowNumber)
//                                    .message(getErrorMessage(e))
//                                    .build()
//                    );
//                }
//            }
//
//        } catch (Exception e) {
//
//            if (e instanceof IllegalArgumentException) {
//                throw (IllegalArgumentException) e;
//            }
//
//            throw new IllegalArgumentException(
//                    "Failed to read Excel file: "
//                            + getErrorMessage(e)
//            );
//        }
//
//        return ExamResultImportResponseDto.builder()
//                .totalRows(totalRows)
//                .created(created)
//                .updated(updated)
//                .errors(errors.size())
//                .errorDetails(errors)
//                .build();
//    }
//
//
//    private void validateExamResultHeader(Row headerRow) {
//
//        String[] expectedHeaders = {
//                "ID",
//                "Exam Schedule ID",
//                "Student ID",
//                "Teacher ID",
//                "Marks Obtained",
//                "Remarks",
//                "Status"
//        };
//
//        for (int i = 0; i < expectedHeaders.length; i++) {
//
//            String actual =
//                    getStringValue(headerRow.getCell(i));
//
//            if (!expectedHeaders[i].equalsIgnoreCase(actual)) {
//                throw new IllegalArgumentException(
//                        "Invalid Excel header at column "
//                                + (i + 1)
//                                + ". Expected: "
//                                + expectedHeaders[i]
//                );
//            }
//        }
//    }


    @Override
    @Transactional
    public GradeRuleImportResponseDto importGradeRulesExcel(MultipartFile file) {

        List<GradeRuleImportErrorDto> errors = new ArrayList<>();

        int totalRows = 0;
        int created = 0;
        int updated = 0;

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is empty");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx Excel files are supported");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null) {
                throw new IllegalArgumentException("Excel sheet is missing");
            }

            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalArgumentException("Excel header row is missing");
            }

            validateGradeRuleHeader(headerRow);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (isRowEmpty(row)) {
                    continue;
                }

                totalRows++;

                int excelRowNumber = rowIndex + 1;

                try {

                    Long id = getLongValue(row.getCell(0));

                    String grade = getStringValue(row.getCell(1));

                    Double minimumPercentage = getDoubleValue(row.getCell(2));

                    Double maximumPercentage = getDoubleValue(row.getCell(3));

                    if (grade == null || grade.isBlank()) {
                        throw new IllegalArgumentException("Grade is required");
                    }

                    if (minimumPercentage == null) {
                        throw new IllegalArgumentException(
                                "Minimum Percentage is required"
                        );
                    }

                    if (maximumPercentage == null) {
                        throw new IllegalArgumentException(
                                "Maximum Percentage is required"
                        );
                    }

                    if (minimumPercentage < 0 || minimumPercentage > 100) {
                        throw new IllegalArgumentException(
                                "Minimum Percentage must be between 0 and 100"
                        );
                    }

                    if (maximumPercentage < 0 || maximumPercentage > 100) {
                        throw new IllegalArgumentException(
                                "Maximum Percentage must be between 0 and 100"
                        );
                    }

                    if (minimumPercentage > maximumPercentage) {
                        throw new IllegalArgumentException(
                                "Minimum Percentage cannot exceed Maximum Percentage"
                        );
                    }

                    GradeRule gradeRule;

                    if (id == null) {

                        gradeRule = GradeRule.builder()
                                .grade(grade.trim())
                                .minimumPercentage(minimumPercentage)
                                .maximumPercentage(maximumPercentage)
                                .build();

                        validateGradeRuleOverlap(
                                minimumPercentage,
                                maximumPercentage,
                                null
                        );

                        gradeRuleRepository.save(gradeRule);

                        created++;

                    } else {

                        gradeRule = gradeRuleRepository.findById(id)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Grade Rule not found with ID: " + id
                                        )
                                );

                        validateGradeRuleOverlap(
                                minimumPercentage,
                                maximumPercentage,
                                id
                        );

                        gradeRule.setGrade(grade.trim());
                        gradeRule.setMinimumPercentage(minimumPercentage);
                        gradeRule.setMaximumPercentage(maximumPercentage);

                        gradeRuleRepository.save(gradeRule);

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

        } catch (Exception e) {

            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }

            throw new IllegalArgumentException(
                    "Failed to read Excel file: " + getErrorMessage(e)
            );
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
                "ID",
                "Grade",
                "Minimum Percentage",
                "Maximum Percentage"
        };

        for (int i = 0; i < expectedHeaders.length; i++) {

            String actual = getStringValue(headerRow.getCell(i));

            if (!expectedHeaders[i].equalsIgnoreCase(actual)) {

                throw new IllegalArgumentException(
                        "Invalid Excel header at column " + (i + 1)
                                + ". Expected: " + expectedHeaders[i]
                );
            }
        }
    }

    private void validateGradeRuleOverlap(
            Double minimumPercentage,
            Double maximumPercentage,
            Long currentId) {

        boolean overlaps = gradeRuleRepository.findAll()
                .stream()
                .anyMatch(existing -> {

                    if (currentId != null
                            && existing.getId().equals(currentId)) {
                        return false;
                    }

                    return minimumPercentage <= existing.getMaximumPercentage()
                            && maximumPercentage >= existing.getMinimumPercentage();
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



//    @Override
//    @Transactional
//    public ExamScheduleImportResponseDto importExamSchedulesExcel(MultipartFile file) {
//
//        List<ExamScheduleImportErrorDto> errors = new ArrayList<>();
//
//        int totalRows = 0;
//        int created = 0;
//        int updated = 0;
//
//        if (file == null || file.isEmpty()) {
//            throw new IllegalArgumentException("Excel file is empty");
//        }
//
//        String filename = file.getOriginalFilename();
//
//        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
//            throw new IllegalArgumentException("Only .xlsx Excel files are supported");
//        }
//
//        try (InputStream inputStream = file.getInputStream();
//             Workbook workbook = new XSSFWorkbook(inputStream)) {
//
//            Sheet sheet = workbook.getSheetAt(0);
//
//            if (sheet == null) {
//                throw new IllegalArgumentException("Excel sheet is missing");
//            }
//
//            Row headerRow = sheet.getRow(0);
//
//            if (headerRow == null) {
//                throw new IllegalArgumentException("Excel header row is missing");
//            }
//
//            validateExamScheduleHeader(headerRow);
//
//            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
//
//                Row row = sheet.getRow(rowIndex);
//
//                if (isRowEmpty(row)) {
//                    continue;
//                }
//
//                totalRows++;
//
//                int excelRowNumber = rowIndex + 1;
//
//                try {
//
//                    Long id = getLongValue(row.getCell(0));
//                    Long examinationId = getLongValue(row.getCell(1));
//                    Long classId = getLongValue(row.getCell(2));
//                    Long sectionId = getLongValue(row.getCell(3));
//                    Long subjectId = getLongValue(row.getCell(4));
//                    Long teacherId = getLongValue(row.getCell(5));
//
//                    LocalDate examDate = getLocalDateValue(row.getCell(6));
//                    LocalTime startTime = getLocalTimeValue(row.getCell(7));
//
//                    Integer duration = getIntegerValue(row.getCell(8));
//                    Integer maxMarks = getIntegerValue(row.getCell(9));
//
//                    String examType = getStringValue(row.getCell(10));
//                    String roomNumber = getStringValue(row.getCell(11));
//                    String status = getStringValue(row.getCell(12));
//
//                    if (examinationId == null) {
//                        throw new IllegalArgumentException(
//                                "Examination ID is required"
//                        );
//                    }
//
//                    if (classId == null) {
//                        throw new IllegalArgumentException(
//                                "Class ID is required"
//                        );
//                    }
//
//                    if (subjectId == null) {
//                        throw new IllegalArgumentException(
//                                "Subject ID is required"
//                        );
//                    }
//
//                    if (examDate == null) {
//                        throw new IllegalArgumentException(
//                                "Exam Date is required"
//                        );
//                    }
//
//                    if (startTime == null) {
//                        throw new IllegalArgumentException(
//                                "Start Time is required"
//                        );
//                    }
//
//                    if (duration == null) {
//                        throw new IllegalArgumentException(
//                                "Duration is required"
//                        );
//                    }
//
//                    if (maxMarks == null) {
//                        throw new IllegalArgumentException(
//                                "Max Marks is required"
//                        );
//                    }
//
//                    if (examType == null || examType.isBlank()) {
//                        throw new IllegalArgumentException(
//                                "Exam Type is required"
//                        );
//                    }
//
//                    ExamScheduleRequestDto request =
//                            ExamScheduleRequestDto.builder()
//                                    .examinationId(examinationId)
//                                    .classId(classId)
//                                    .sectionId(sectionId)
//                                    .subjectTeacherId(teacherId)
//                                    .subjectId(subjectId)
//                                    .examDate(examDate)
//                                    .startTime(startTime)
//                                    .duration(duration)
//                                    .maxMarks(maxMarks)
//                                    .examType(examType)
//                                    .roomNumber(roomNumber)
//                                    .status(status)
//                                    .build();
//
//                    if (id == null) {
//
//                        createSchedule(request);
//
//                        created++;
//
//                    } else {
//
//                        findSchedule(id);
//
//                        updateSchedule(id, request);
//
//                        updated++;
//                    }
//
//                } catch (Exception e) {
//
//                    errors.add(
//                            ExamScheduleImportErrorDto.builder()
//                                    .row(excelRowNumber)
//                                    .message(getErrorMessage(e))
//                                    .build()
//                    );
//                }
//            }
//
//        } catch (Exception e) {
//
//            if (e instanceof IllegalArgumentException) {
//                throw (IllegalArgumentException) e;
//            }
//
//            throw new IllegalArgumentException(
//                    "Failed to read Excel file: " + getErrorMessage(e)
//            );
//        }
//
//        return ExamScheduleImportResponseDto.builder()
//                .totalRows(totalRows)
//                .created(created)
//                .updated(updated)
//                .errors(errors.size())
//                .errorDetails(errors)
//                .build();
//    }
//
//
//
//    private void validateExamScheduleHeader(Row headerRow) {
//
//        String[] expectedHeaders = {
//                "ID",
//                "Examination ID",
//                "Class ID",
//                "Section ID",
//                "Subject ID",
//                "Teacher ID",
//                "Exam Date",
//                "Start Time",
//                "Duration",
//                "Max Marks",
//                "Exam Type",
//                "Room Number",
//                "Status"
//        };
//
//        for (int i = 0; i < expectedHeaders.length; i++) {
//
//            String actual = getStringValue(headerRow.getCell(i));
//
//            if (!expectedHeaders[i].equalsIgnoreCase(actual)) {
//
//                throw new IllegalArgumentException(
//                        "Invalid Excel header at column " + (i + 1)
//                                + ". Expected: " + expectedHeaders[i]
//                );
//            }
//        }
//    }

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

        if (to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "End date must not precede start date"
            );
        }

        return scheduleRepository
                .findByExamDateBetween(from, to, pageable)
                .map(this::scheduleDto);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ExamResultResponseDto> getAcademicPerformanceReport(
            Pageable pageable) {

        return resultRepository
                .findAll(pageable)
                .map(this::resultDto);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ExamResultResponseDto> getStudentPerformanceReport(
            Long studentId,
            Pageable pageable,
            boolean staff) {

        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(
                    "Student not found: " + studentId
            );
        }

        Page<ExamResult> resultPage =
                staff
                        ? resultRepository.findByStudentId(
                        studentId,
                        pageable
                )
                        : resultRepository.findByStudentIdAndIsPublishedTrue(
                        studentId,
                        pageable
                );

        return resultPage.map(this::resultDto);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ExamScheduleResponseDto> getTeacherClassPerformanceReport(
            Long classId,
            Pageable pageable) {

        return scheduleRepository
                .findByClassEntityId(classId, pageable)
                .map(this::scheduleDto);
    }



    @Override
    public List<ExamResultResponseDto> setStudentResultsPublication(
            Long studentId,
            Long examinationId,
            boolean published) {

        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(
                    "Student not found: " + studentId
            );
        }

        Examination examination = findExam(examinationId);

        List<ExamResult> results =
                resultRepository.findByStudentId(studentId)
                        .stream()
                        .filter(result ->
                                result.getExamSchedule() != null
                                        && result.getExamSchedule()
                                        .getExamination() != null
                                        && result.getExamSchedule()
                                        .getExamination()
                                        .getId()
                                        .equals(examination.getId())
                        )
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
    public List<ExamResultResponseDto> setExaminationResultsPublication(
            Long examinationId,
            boolean published) {

        Examination examination = findExam(examinationId);

        List<ExamResult> results =
                resultRepository.findAll()
                        .stream()
                        .filter(result ->
                                result.getExamSchedule() != null
                                        && result.getExamSchedule()
                                        .getExamination() != null
                                        && result.getExamSchedule()
                                        .getExamination()
                                        .getId()
                                        .equals(examination.getId())
                        )
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


}

