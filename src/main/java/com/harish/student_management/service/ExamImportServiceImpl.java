package com.harish.student_management.service;

import com.harish.student_management.dto.ExaminationImportErrorDto;
import com.harish.student_management.dto.ExaminationImportResponseDto;
import com.harish.student_management.dto.ExamResultImportResponseDto;
import com.harish.student_management.dto.ExamScheduleImportResponseDto;
import com.harish.student_management.entity.*;
import com.harish.student_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.harish.student_management.dto.ExamResultImportErrorDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamImportServiceImpl implements ExamImportService {

    private final ExaminationRepository examinationRepository;
    private final AcademicYearRepository academicYearRepository;

    private final ExamScheduleRepository scheduleRepository;
    private final ExamResultRepository resultRepository;

    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    private final ClassSubjectRepository classSubjectRepository;
    private final TeacherAccessService teacherAccessService;

    private final GradeRuleRepository gradeRuleRepository;


    // =========================================================
    // EXAMINATION IMPORT
    // =========================================================

    @Override
    public ExaminationImportResponseDto importExaminationsExcel(
            MultipartFile file) {

        int totalRows = 0;
        int created = 0;
        int updated = 0;
        int errorCount = 0;

        List<ExaminationImportErrorDto> errorDetails =
                new ArrayList<>();

        try (Workbook workbook =
                     WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                totalRows++;

                try {

                    String academicYearName =
                            getCellString(row.getCell(0));

                    String name =
                            getCellString(row.getCell(1));

                    String description =
                            getCellString(row.getCell(2));

                    String examType =
                            getCellString(row.getCell(3));

                    Integer year =
                            getCellInteger(row.getCell(4));

                    String status =
                            getCellString(row.getCell(5));


                    // -------------------------------------------------
                    // Required validation
                    // -------------------------------------------------

                    if (academicYearName == null ||
                            academicYearName.isBlank()) {

                        throw new IllegalArgumentException(
                                "Academic Year is required"
                        );
                    }

                    if (name == null || name.isBlank()) {

                        throw new IllegalArgumentException(
                                "Examination Name is required"
                        );
                    }

                    if (examType == null || examType.isBlank()) {

                        throw new IllegalArgumentException(
                                "Exam Type is required"
                        );
                    }


                    // -------------------------------------------------
                    // Academic Year
                    // -------------------------------------------------

                    AcademicYear academicYear =
                            academicYearRepository
                                    .findByNameIgnoreCase(
                                            academicYearName.trim()
                                    )
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "Academic Year not found: "
                                                            + academicYearName
                                            ));


                    // -------------------------------------------------
                    // Existing Examination
                    //
                    // Same Academic Year + Name
                    // = Update
                    // Otherwise = Create
                    // -------------------------------------------------

                    Examination examination =
                            examinationRepository
                                    .findByAcademicYearIdAndNameIgnoreCase(
                                            academicYear.getId(),
                                            name.trim()
                                    )
                                    .orElse(null);


                    if (examination == null) {

                        // ---------------------------------------------
                        // CREATE
                        // ---------------------------------------------

                        examination =
                                Examination.builder()
                                        .academicYear(academicYear)
                                        .name(name.trim())
                                        .description(description)
                                        .examType(examType.trim())
                                        .year(year)
                                        .status(
                                                status == null ||
                                                        status.isBlank()
                                                        ? "DRAFT"
                                                        : status.trim()
                                        )
                                        .build();

                        examinationRepository.save(examination);

                        created++;

                    } else {

                        // ---------------------------------------------
                        // UPDATE
                        // ---------------------------------------------

                        examination.setAcademicYear(academicYear);
                        examination.setName(name.trim());
                        examination.setDescription(description);
                        examination.setExamType(examType.trim());
                        examination.setYear(year);

                        if (status != null &&
                                !status.isBlank()) {

                            examination.setStatus(
                                    status.trim()
                            );
                        }

                        examinationRepository.save(examination);

                        updated++;
                    }


                } catch (Exception e) {

                    errorCount++;

                    errorDetails.add(
                            ExaminationImportErrorDto.builder()
                                    .row(i + 1)
                                    .message(
                                            e.getMessage() != null
                                                    ? e.getMessage()
                                                    : "Unknown error"
                                    )
                                    .build()
                    );
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to read Examination Excel file",
                    e
            );
        }


        return ExaminationImportResponseDto.builder()
                .totalRows(totalRows)
                .created(created)
                .updated(updated)
                .errors(errorCount)
                .errorDetails(errorDetails)
                .build();
    }


    // =========================================================
    // EXAM SCHEDULE IMPORT
    // =========================================================

    @Override
    public ExamScheduleImportResponseDto importExamSchedulesExcel(
            MultipartFile file) {

        int totalRows = 0;
        int created = 0;
        int updated = 0;
        int errorCount = 0;

        List<String> errors = new ArrayList<>();

        System.out.println("========================================");
        System.out.println("EXAM SCHEDULE IMPORT STARTED");
        System.out.println("FILE NAME: " + file.getOriginalFilename());
        System.out.println("FILE SIZE: " + file.getSize());
        System.out.println("========================================");

        try (Workbook workbook =
                     WorkbookFactory.create(file.getInputStream())) {

            System.out.println("WORKBOOK CREATED SUCCESSFULLY");
            System.out.println("NUMBER OF SHEETS: " + workbook.getNumberOfSheets());

            Sheet sheet = workbook.getSheetAt(0);

            System.out.println("SHEET NAME: " + sheet.getSheetName());
            System.out.println("LAST ROW NUMBER: " + sheet.getLastRowNum());
            System.out.println("PHYSICAL ROWS: " + sheet.getPhysicalNumberOfRows());

            // ---------------------------------------------------------
            // PRINT HEADER
            // ---------------------------------------------------------

            Row headerRow = sheet.getRow(0);

            if (headerRow != null) {

                System.out.println("---------- EXCEL HEADER ----------");

                for (int c = 0; c <= 10; c++) {

                    String value =
                            getCellString(headerRow.getCell(c));

                    System.out.println(
                            "COLUMN " + c + " = [" + value + "]"
                    );
                }

                System.out.println("----------------------------------");
            } else {

                System.out.println("WARNING: HEADER ROW IS NULL");
            }


            // ---------------------------------------------------------
            // ROW LOOP
            // ---------------------------------------------------------

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                System.out.println();
                System.out.println("========================================");
                System.out.println("PROCESSING EXCEL ROW: " + (i + 1));
                System.out.println("========================================");

                Row row = sheet.getRow(i);

                if (row == null) {

                    System.out.println(
                            "ROW " + (i + 1) + " IS NULL -> SKIPPING"
                    );

                    continue;
                }

                System.out.println(
                        "ROW " + (i + 1) + " OBJECT FOUND"
                );

                System.out.println(
                        "ROW FIRST CELL: [" +
                                getCellString(row.getCell(0)) +
                                "]"
                );

                System.out.println(
                        "IS ROW EMPTY: " + isRowEmpty(row)
                );

                if (isRowEmpty(row)) {

                    System.out.println(
                            "ROW " + (i + 1) +
                                    " IS EMPTY -> SKIPPING"
                    );

                    continue;
                }

                totalRows++;

                System.out.println(
                        "TOTAL ROWS COUNT NOW: " + totalRows
                );

                try {

                    // -------------------------------------------------
                    // READ CELLS
                    // -------------------------------------------------

                    String examinationName =
                            getCellString(row.getCell(0));

                    String className =
                            getCellString(row.getCell(1));

                    String sectionName =
                            getCellString(row.getCell(2));

                    String subjectName =
                            getCellString(row.getCell(3));

                    String teacherName =
                            getCellString(row.getCell(4));

                    LocalDate examDate =
                            getLocalDate(row.getCell(5));

                    LocalTime startTime =
                            getLocalTime(row.getCell(6));

                    Integer duration =
                            getCellInteger(row.getCell(7));

                    Integer maxMarks =
                            getCellInteger(row.getCell(8));

                    String roomNumber =
                            getCellString(row.getCell(9));

                    String status =
                            getCellString(row.getCell(10));


                    System.out.println("---------- ROW DATA ----------");

                    System.out.println(
                            "Examination = [" +
                                    examinationName + "]"
                    );

                    System.out.println(
                            "Class = [" +
                                    className + "]"
                    );

                    System.out.println(
                            "Section = [" +
                                    sectionName + "]"
                    );

                    System.out.println(
                            "Subject = [" +
                                    subjectName + "]"
                    );

                    System.out.println(
                            "Teacher = [" +
                                    teacherName + "]"
                    );

                    System.out.println(
                            "Exam Date = [" +
                                    examDate + "]"
                    );

                    System.out.println(
                            "Start Time = [" +
                                    startTime + "]"
                    );

                    System.out.println(
                            "Duration = [" +
                                    duration + "]"
                    );

                    System.out.println(
                            "Max Marks = [" +
                                    maxMarks + "]"
                    );

                    System.out.println(
                            "Room Number = [" +
                                    roomNumber + "]"
                    );

                    System.out.println(
                            "Status = [" +
                                    status + "]"
                    );

                    System.out.println("-------------------------------");


                    // -------------------------------------------------
                    // REQUIRED VALIDATION
                    // -------------------------------------------------

                    if (examinationName == null ||
                            examinationName.isBlank()) {

                        throw new IllegalArgumentException(
                                "Examination is required"
                        );
                    }

                    if (className == null ||
                            className.isBlank()) {

                        throw new IllegalArgumentException(
                                "Class is required"
                        );
                    }

                    if (subjectName == null ||
                            subjectName.isBlank()) {

                        throw new IllegalArgumentException(
                                "Subject is required"
                        );
                    }

                    if (examDate == null) {

                        throw new IllegalArgumentException(
                                "Exam Date is required"
                        );
                    }

                    if (startTime == null) {

                        throw new IllegalArgumentException(
                                "Start Time is required"
                        );
                    }

                    if (duration == null ||
                            duration <= 0) {

                        throw new IllegalArgumentException(
                                "Duration must be greater than 0"
                        );
                    }


                    // -------------------------------------------------
                    // EXAMINATION
                    // -------------------------------------------------

                    System.out.println(
                            "LOOKING FOR EXAMINATION: [" +
                                    examinationName + "]"
                    );

                    Examination examination =
                            examinationRepository
                                    .findByNameIgnoreCase(
                                            examinationName.trim()
                                    )
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "Examination not found: "
                                                            + examinationName
                                            ));

                    System.out.println(
                            "EXAMINATION FOUND: ID=" +
                                    examination.getId() +
                                    ", NAME=" +
                                    examination.getName()
                    );


                    // -------------------------------------------------
                    // CLASS
                    // -------------------------------------------------

                    System.out.println(
                            "LOOKING FOR CLASS: [" +
                                    className + "]"
                    );

                    ClassEntity clazz =
                            classRepository
                                    .findByNameIgnoreCase(
                                            className.trim()
                                    )
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "Class not found: "
                                                            + className
                                            ));

                    System.out.println(
                            "CLASS FOUND: ID=" +
                                    clazz.getId() +
                                    ", NAME=" +
                                    clazz.getName()
                    );


                    // -------------------------------------------------
                    // SECTION OPTIONAL
                    // -------------------------------------------------

                    Section section = null;

                    if (sectionName != null &&
                            !sectionName.isBlank()) {

                        System.out.println(
                                "LOOKING FOR SECTION: [" +
                                        sectionName +
                                        "] FOR CLASS ID=" +
                                        clazz.getId()
                        );

                        section =
                                sectionRepository
                                        .findByNameIgnoreCaseAndClassEntityId(
                                                sectionName.trim(),
                                                clazz.getId()
                                        )
                                        .orElseThrow(() ->
                                                new IllegalArgumentException(
                                                        "Section not found: "
                                                                + sectionName
                                                                + " for class "
                                                                + className
                                                ));

                        System.out.println(
                                "SECTION FOUND: ID=" +
                                        section.getId() +
                                        ", NAME=" +
                                        section.getName()
                        );

                    } else {

                        System.out.println(
                                "SECTION IS BLANK -> NULL"
                        );
                    }


                    // -------------------------------------------------
                    // SUBJECT
                    // -------------------------------------------------

                    System.out.println(
                            "LOOKING FOR SUBJECT: [" +
                                    subjectName + "]"
                    );

                    Subject subject =
                            subjectRepository
                                    .findByNameIgnoreCase(
                                            subjectName.trim()
                                    )
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "Subject not found: "
                                                            + subjectName
                                            ));

                    System.out.println(
                            "SUBJECT FOUND: ID=" +
                                    subject.getId() +
                                    ", NAME=" +
                                    subject.getName()
                    );


                    // -------------------------------------------------
                    // SCHOOL VALIDATION
                    // -------------------------------------------------

                    System.out.println(
                            "VALIDATING SUBJECT/CLASS SCHOOL"
                    );

                    if (subject.getSchool() == null ||
                            clazz.getSchool() == null ||
                            !subject.getSchool()
                                    .getId()
                                    .equals(
                                            clazz.getSchool().getId()
                                    )) {

                        throw new IllegalArgumentException(
                                "Subject does not belong to the same school as the class"
                        );
                    }

                    System.out.println(
                            "SCHOOL VALIDATION PASSED"
                    );


                    // -------------------------------------------------
                    // SUBJECT ASSIGNED TO CLASS
                    // -------------------------------------------------

                    System.out.println(
                            "CHECKING SUBJECT ASSIGNMENT: " +
                                    "CLASS ID=" + clazz.getId() +
                                    ", SUBJECT ID=" + subject.getId()
                    );

                    boolean assignedToClass =
                            classSubjectRepository
                                    .existsByClassEntityIdAndSubjectId(
                                            clazz.getId(),
                                            subject.getId()
                                    );

                    System.out.println(
                            "SUBJECT ASSIGNED TO CLASS: " +
                                    assignedToClass
                    );

                    if (!assignedToClass) {

                        throw new IllegalArgumentException(
                                "Subject is not assigned to the selected class"
                        );
                    }


                    // -------------------------------------------------
                    // TEACHER OPTIONAL
                    // -------------------------------------------------

                    Teacher teacher = null;

                    if (teacherName != null &&
                            !teacherName.isBlank()) {

                        System.out.println(
                                "LOOKING FOR TEACHER: [" +
                                        teacherName + "]"
                        );

                        teacher =
                                teacherRepository
                                        .findByNameIgnoreCase(
                                                teacherName.trim()
                                        )
                                        .orElseThrow(() ->
                                                new IllegalArgumentException(
                                                        "Teacher not found: "
                                                                + teacherName
                                                ));

                        System.out.println(
                                "TEACHER FOUND: ID=" +
                                        teacher.getId() +
                                        ", NAME=" +
                                        teacher.getName()
                        );


                        System.out.println(
                                "VALIDATING TEACHER ACCESS"
                        );

                        teacherAccessService
                                .validateTeacherAccessToClassAndSubject(
                                        teacher.getId(),
                                        clazz.getId(),
                                        subject.getId()
                                );

                        System.out.println(
                                "TEACHER ACCESS VALIDATION PASSED"
                        );

                    } else {

                        System.out.println(
                                "TEACHER IS BLANK -> NULL"
                        );
                    }


                    // -------------------------------------------------
                    // EXISTING SCHEDULE
                    // -------------------------------------------------

                    Long sectionId =
                            section != null
                                    ? section.getId()
                                    : null;

                    System.out.println(
                            "CHECKING EXISTING SCHEDULE"
                    );

                    System.out.println(
                            "Examination ID = " +
                                    examination.getId()
                    );

                    System.out.println(
                            "Class ID = " +
                                    clazz.getId()
                    );

                    System.out.println(
                            "Section ID = " +
                                    sectionId
                    );

                    System.out.println(
                            "Subject ID = " +
                                    subject.getId()
                    );

                    System.out.println(
                            "Exam Date = " +
                                    examDate
                    );


                    ExamSchedule schedule =
                            scheduleRepository
                                    .findExistingSchedule(
                                            examination.getId(),
                                            clazz.getId(),
                                            sectionId,
                                            subject.getId(),
                                            examDate
                                    )
                                    .orElse(null);


                    // -------------------------------------------------
                    // CREATE
                    // -------------------------------------------------

                    if (schedule == null) {

                        System.out.println(
                                "NO EXISTING SCHEDULE FOUND -> CREATE"
                        );

                        schedule =
                                ExamSchedule.builder()
                                        .examination(examination)
                                        .classEntity(clazz)
                                        .section(section)
                                        .subjectTeacher(teacher)
                                        .subject(subject)
                                        .examDate(examDate)
                                        .startTime(startTime)
                                        .duration(duration)
                                        .maxMarks(maxMarks)
                                        .examType(
                                                examination.getExamType()
                                        )
                                        .roomNumber(roomNumber)
                                        .status(
                                                status == null ||
                                                        status.isBlank()
                                                        ? "SCHEDULED"
                                                        : status.trim()
                                        )
                                        .build();

                        scheduleRepository.save(schedule);

                        created++;

                        System.out.println(
                                "SCHEDULE CREATED SUCCESSFULLY"
                        );

                        System.out.println(
                                "CREATED SCHEDULE ID: " +
                                        schedule.getId()
                        );

                        System.out.println(
                                "CREATED COUNT: " +
                                        created
                        );

                    } else {

                        // -------------------------------------------------
                        // UPDATE
                        // -------------------------------------------------

                        System.out.println(
                                "EXISTING SCHEDULE FOUND: ID=" +
                                        schedule.getId() +
                                        " -> UPDATE"
                        );

                        schedule.setExamination(examination);
                        schedule.setClassEntity(clazz);
                        schedule.setSection(section);
                        schedule.setSubject(subject);

                        schedule.setSubjectTeacher(teacher);

                        schedule.setExamDate(examDate);
                        schedule.setStartTime(startTime);
                        schedule.setDuration(duration);
                        schedule.setMaxMarks(maxMarks);

                        schedule.setExamType(
                                examination.getExamType()
                        );

                        schedule.setRoomNumber(roomNumber);

                        if (status != null &&
                                !status.isBlank()) {

                            schedule.setStatus(
                                    status.trim()
                            );
                        }

                        scheduleRepository.save(schedule);

                        updated++;

                        System.out.println(
                                "SCHEDULE UPDATED SUCCESSFULLY"
                        );

                        System.out.println(
                                "UPDATED SCHEDULE ID: " +
                                        schedule.getId()
                        );

                        System.out.println(
                                "UPDATED COUNT: " +
                                        updated
                        );
                    }


                    System.out.println(
                            "ROW " + (i + 1) +
                                    " PROCESSED SUCCESSFULLY"
                    );


                } catch (Exception e) {

                    errorCount++;

                    String errorMessage =
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Unknown error";

                    errors.add(
                            "Row " + (i + 1) +
                                    ": " +
                                    errorMessage
                    );

                    System.out.println(
                            "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
                    );

                    System.out.println(
                            "ERROR PROCESSING ROW: " +
                                    (i + 1)
                    );

                    System.out.println(
                            "ERROR MESSAGE: " +
                                    errorMessage
                    );

                    e.printStackTrace();

                    System.out.println(
                            "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
                    );
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
            );

            System.out.println(
                    "FAILED TO READ EXAM SCHEDULE EXCEL"
            );

            System.out.println(
                    "ERROR: " +
                            e.getMessage()
            );

            e.printStackTrace();

            System.out.println(
                    "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
            );

            throw new RuntimeException(
                    "Failed to read Exam Schedule Excel file",
                    e
            );
        }


        // ---------------------------------------------------------
        // FINAL IMPORT SUMMARY
        // ---------------------------------------------------------

        System.out.println();
        System.out.println("========================================");
        System.out.println("EXAM SCHEDULE IMPORT FINISHED");
        System.out.println("TOTAL ROWS: " + totalRows);
        System.out.println("CREATED: " + created);
        System.out.println("UPDATED: " + updated);
        System.out.println("ERRORS: " + errorCount);
        System.out.println("ERROR DETAILS: " + errors);
        System.out.println("========================================");


        /*
         * IMPORTANT:
         *
         * Nee ExamScheduleImportResponseDto fields exact ga
         * ikkada use cheyyali.
         */

        return ExamScheduleImportResponseDto.builder()
                // replace with your actual DTO fields
                .build();
    }
    // =========================================================
    // EXAM RESULT IMPORT
    // =========================================================

    @Override
    public ExamResultImportResponseDto importExamResultsExcel(
            MultipartFile file) {

        int totalRows = 0;
        int created = 0;
        int updated = 0;
        int errorCount = 0;

        List<ExamResultImportErrorDto> errors = new ArrayList<>();

        Set<Long> affectedScheduleIds = new HashSet<>();

        System.out.println();
        System.out.println("==================================================");
        System.out.println("          EXAM RESULT IMPORT STARTED");
        System.out.println("==================================================");

        System.out.println("FILE NAME       : " + file.getOriginalFilename());
        System.out.println("FILE SIZE       : " + file.getSize());
        System.out.println("CONTENT TYPE    : " + file.getContentType());
        System.out.println("IS EMPTY        : " + file.isEmpty());

        System.out.println("==================================================");

        try (Workbook workbook =
                     WorkbookFactory.create(file.getInputStream())) {

            System.out.println("WORKBOOK CREATED SUCCESSFULLY");
            System.out.println(
                    "NUMBER OF SHEETS: " +
                            workbook.getNumberOfSheets()
            );

            // ---------------------------------------------------------
            // SHEET INFORMATION
            // ---------------------------------------------------------

            for (int s = 0;
                 s < workbook.getNumberOfSheets();
                 s++) {

                Sheet debugSheet =
                        workbook.getSheetAt(s);

                System.out.println(
                        "SHEET[" + s + "] NAME       : " +
                                debugSheet.getSheetName()
                );

                System.out.println(
                        "SHEET[" + s + "] LAST ROW   : " +
                                debugSheet.getLastRowNum()
                );

                System.out.println(
                        "SHEET[" + s + "] PHYSICAL   : " +
                                debugSheet.getPhysicalNumberOfRows()
                );

                System.out.println(
                        "SHEET[" + s + "] FIRST ROW  : " +
                                debugSheet.getFirstRowNum()
                );
            }

            System.out.println("==================================================");

            Sheet sheet = workbook.getSheetAt(0);

            System.out.println(
                    "USING SHEET: [" +
                            sheet.getSheetName() +
                            "]"
            );

            System.out.println(
                    "FIRST ROW NUMBER: " +
                            sheet.getFirstRowNum()
            );

            System.out.println(
                    "LAST ROW NUMBER: " +
                            sheet.getLastRowNum()
            );

            System.out.println(
                    "PHYSICAL ROW COUNT: " +
                            sheet.getPhysicalNumberOfRows()
            );

            // ---------------------------------------------------------
            // HEADER
            // ---------------------------------------------------------

            Row headerRow = sheet.getRow(0);

            System.out.println();
            System.out.println("==================================================");
            System.out.println("                 HEADER DEBUG");
            System.out.println("==================================================");

            if (headerRow == null) {

                System.out.println(
                        "WARNING: HEADER ROW 0 IS NULL"
                );

            } else {

                System.out.println(
                        "HEADER FIRST CELL: " +
                                headerRow.getFirstCellNum()
                );

                System.out.println(
                        "HEADER LAST CELL : " +
                                headerRow.getLastCellNum()
                );

                for (int c = 0; c <= 9; c++) {

                    Cell cell =
                            headerRow.getCell(c);

                    System.out.println(
                            "COLUMN " + c +
                                    " | CELL TYPE = " +
                                    (
                                            cell != null
                                                    ? cell.getCellType()
                                                    : "NULL"
                                    ) +
                                    " | VALUE = [" +
                                    getCellString(cell) +
                                    "]"
                    );
                }
            }

            System.out.println("==================================================");

            // ---------------------------------------------------------
            // CHECK ALL ROWS BEFORE PROCESSING
            // ---------------------------------------------------------

            System.out.println();
            System.out.println("==================================================");
            System.out.println("              RAW ROW DEBUG");
            System.out.println("==================================================");

            for (int i = 0;
                 i <= sheet.getLastRowNum();
                 i++) {

                Row debugRow =
                        sheet.getRow(i);

                System.out.println();
                System.out.println(
                        "RAW ROW INDEX: " + i +
                                " | EXCEL ROW: " + (i + 1)
                );

                if (debugRow == null) {

                    System.out.println(
                            "ROW OBJECT = NULL"
                    );

                    continue;
                }

                System.out.println(
                        "FIRST CELL INDEX: " +
                                debugRow.getFirstCellNum()
                );

                System.out.println(
                        "LAST CELL INDEX: " +
                                debugRow.getLastCellNum()
                );

                System.out.println(
                        "ROW EMPTY: " +
                                isRowEmpty(debugRow)
                );

                for (int c = 0; c <= 9; c++) {

                    Cell cell =
                            debugRow.getCell(c);

                    String value =
                            getCellString(cell);

                    System.out.println(
                            "  CELL[" + c + "] = [" +
                                    value +
                                    "]"
                    );
                }
            }

            System.out.println();
            System.out.println("==================================================");
            System.out.println("              STARTING ROW IMPORT");
            System.out.println("==================================================");

            // ---------------------------------------------------------
            // ROW LOOP
            // ---------------------------------------------------------

            for (int i = 1;
                 i <= sheet.getLastRowNum();
                 i++) {

                System.out.println();
                System.out.println(
                        "--------------------------------------------------"
                );

                System.out.println(
                        "PROCESSING ROW INDEX: " + i
                );

                System.out.println(
                        "EXCEL ROW NUMBER: " + (i + 1)
                );

                System.out.println(
                        "--------------------------------------------------"
                );

                Row row =
                        sheet.getRow(i);

                if (row == null) {

                    System.out.println(
                            "ROW IS NULL -> SKIPPING"
                    );

                    continue;
                }

                System.out.println(
                        "ROW OBJECT FOUND"
                );

                System.out.println(
                        "FIRST CELL INDEX: " +
                                row.getFirstCellNum()
                );

                System.out.println(
                        "LAST CELL INDEX: " +
                                row.getLastCellNum()
                );

                boolean empty =
                        isRowEmpty(row);

                System.out.println(
                        "IS ROW EMPTY: " +
                                empty
                );

                if (empty) {

                    System.out.println(
                            "ROW EMPTY -> SKIPPING"
                    );

                    continue;
                }

                totalRows++;

                System.out.println(
                        "TOTAL ROWS NOW: " +
                                totalRows
                );

                try {

                    // -------------------------------------------------
                    // READ CELLS
                    // -------------------------------------------------

                    String examinationName =
                            getCellString(
                                    row.getCell(0)
                            );

                    String className =
                            getCellString(
                                    row.getCell(1)
                            );

                    String sectionName =
                            getCellString(
                                    row.getCell(2)
                            );

                    String subjectName =
                            getCellString(
                                    row.getCell(3)
                            );

                    LocalDate examDate =
                            getLocalDate(
                                    row.getCell(4)
                            );

                    String admissionNo =
                            getCellString(
                                    row.getCell(5)
                            );

                    String rollNumber =
                            getCellString(
                                    row.getCell(6)
                            );

                    Integer marksObtained =
                            getCellInteger(
                                    row.getCell(7)
                            );

                    String remarks =
                            getCellString(
                                    row.getCell(8)
                            );

                    String status =
                            getCellString(
                                    row.getCell(9)
                            );

                    // -------------------------------------------------
                    // ROW DATA DEBUG
                    // -------------------------------------------------

                    System.out.println();
                    System.out.println(
                            "========== PARSED ROW DATA =========="
                    );

                    System.out.println(
                            "Examination : [" +
                                    examinationName + "]"
                    );

                    System.out.println(
                            "Class       : [" +
                                    className + "]"
                    );

                    System.out.println(
                            "Section     : [" +
                                    sectionName + "]"
                    );

                    System.out.println(
                            "Subject     : [" +
                                    subjectName + "]"
                    );

                    System.out.println(
                            "Exam Date   : [" +
                                    examDate + "]"
                    );

                    System.out.println(
                            "Admission No: [" +
                                    admissionNo + "]"
                    );

                    System.out.println(
                            "Roll Number : [" +
                                    rollNumber + "]"
                    );

                    System.out.println(
                            "Marks       : [" +
                                    marksObtained + "]"
                    );

                    System.out.println(
                            "Remarks     : [" +
                                    remarks + "]"
                    );

                    System.out.println(
                            "Status      : [" +
                                    status + "]"
                    );

                    System.out.println(
                            "======================================"
                    );

                    // -------------------------------------------------
                    // REQUIRED VALIDATION
                    // -------------------------------------------------

                    if (examinationName == null ||
                            examinationName.isBlank()) {

                        throw new IllegalArgumentException(
                                "Examination is required"
                        );
                    }

                    if (className == null ||
                            className.isBlank()) {

                        throw new IllegalArgumentException(
                                "Class is required"
                        );
                    }

                    if (subjectName == null ||
                            subjectName.isBlank()) {

                        throw new IllegalArgumentException(
                                "Subject is required"
                        );
                    }

                    if (examDate == null) {

                        throw new IllegalArgumentException(
                                "Exam Date is required"
                        );
                    }

                    if (admissionNo == null ||
                            admissionNo.isBlank()) {

                        throw new IllegalArgumentException(
                                "Admission No is required"
                        );
                    }

                    if (rollNumber == null ||
                            rollNumber.isBlank()) {

                        throw new IllegalArgumentException(
                                "Roll Number is required"
                        );
                    }

                    if (marksObtained == null) {

                        throw new IllegalArgumentException(
                                "Marks Obtained is required"
                        );
                    }

                    // -------------------------------------------------
                    // EXAMINATION
                    // -------------------------------------------------

                    System.out.println(
                            "LOOKING FOR EXAMINATION: [" +
                                    examinationName +
                                    "]"
                    );

                    Examination examination =
                            examinationRepository
                                    .findByNameIgnoreCase(
                                            examinationName.trim()
                                    )
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "Examination not found: " +
                                                            examinationName
                                            ));

                    System.out.println(
                            "EXAMINATION FOUND: ID=" +
                                    examination.getId()
                    );

                    // -------------------------------------------------
                    // CLASS
                    // -------------------------------------------------

                    System.out.println(
                            "LOOKING FOR CLASS: [" +
                                    className +
                                    "]"
                    );

                    ClassEntity clazz =
                            classRepository
                                    .findByNameIgnoreCase(
                                            className.trim()
                                    )
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "Class not found: " +
                                                            className
                                            ));

                    System.out.println(
                            "CLASS FOUND: ID=" +
                                    clazz.getId()
                    );

                    // -------------------------------------------------
                    // SECTION
                    // -------------------------------------------------

                    Section section = null;

                    if (sectionName != null &&
                            !sectionName.isBlank()) {

                        System.out.println(
                                "LOOKING FOR SECTION: [" +
                                        sectionName +
                                        "]"
                        );

                        section =
                                sectionRepository
                                        .findByNameIgnoreCaseAndClassEntityId(
                                                sectionName.trim(),
                                                clazz.getId()
                                        )
                                        .orElseThrow(() ->
                                                new IllegalArgumentException(
                                                        "Section not found: " +
                                                                sectionName
                                                ));

                        System.out.println(
                                "SECTION FOUND: ID=" +
                                        section.getId()
                        );
                    }

                    // -------------------------------------------------
                    // SUBJECT
                    // -------------------------------------------------

                    System.out.println(
                            "LOOKING FOR SUBJECT: [" +
                                    subjectName +
                                    "]"
                    );

                    Subject subject =
                            subjectRepository
                                    .findByNameIgnoreCase(
                                            subjectName.trim()
                                    )
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "Subject not found: " +
                                                            subjectName
                                            ));

                    System.out.println(
                            "SUBJECT FOUND: ID=" +
                                    subject.getId()
                    );

                    // -------------------------------------------------
                    // SCHEDULE
                    // -------------------------------------------------

                    Long sectionId =
                            section != null
                                    ? section.getId()
                                    : null;

                    System.out.println(
                            "LOOKING FOR SCHEDULE:"
                    );

                    System.out.println(
                            "  Examination ID = " +
                                    examination.getId()
                    );

                    System.out.println(
                            "  Class ID       = " +
                                    clazz.getId()
                    );

                    System.out.println(
                            "  Section ID     = " +
                                    sectionId
                    );

                    System.out.println(
                            "  Subject ID     = " +
                                    subject.getId()
                    );

                    System.out.println(
                            "  Exam Date      = " +
                                    examDate
                    );

                    ExamSchedule schedule =
                            scheduleRepository
                                    .findExistingSchedule(
                                            examination.getId(),
                                            clazz.getId(),
                                            sectionId,
                                            subject.getId(),
                                            examDate
                                    )
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "Exam schedule not found"
                                            ));

                    System.out.println(
                            "SCHEDULE FOUND: ID=" +
                                    schedule.getId()
                    );

                    System.out.println(
                            "SCHEDULE MAX MARKS: " +
                                    schedule.getMaxMarks()
                    );

                    // -------------------------------------------------
                    // STUDENT
                    // -------------------------------------------------

                    System.out.println(
                            "LOOKING FOR STUDENT: [" +
                                    admissionNo +
                                    "]"
                    );

                    Student student =
                            studentRepository
                                    .findByAdmissionNoIgnoreCase(
                                            admissionNo.trim()
                                    )
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "Student not found: " +
                                                            admissionNo
                                            ));

                    System.out.println(
                            "STUDENT FOUND:"
                    );

                    System.out.println(
                            "  ID           = " +
                                    student.getId()
                    );

                    System.out.println(
                            "  Admission No = " +
                                    student.getAdmissionNo()
                    );

                    System.out.println(
                            "  DB Roll No   = " +
                                    student.getRollNumber()
                    );

                    System.out.println(
                            "  Excel Roll   = " +
                                    rollNumber
                    );

                    System.out.println(
                            "  Section      = " +
                                    (
                                            student.getSection() != null
                                                    ? student.getSection().getName()
                                                    : "NULL"
                                    )
                    );

                    // -------------------------------------------------
                    // ROLL VALIDATION
                    // -------------------------------------------------

                    if (student.getRollNumber() == null ||
                            student.getRollNumber().isBlank()) {

                        throw new IllegalArgumentException(
                                "Roll Number not configured for student: " +
                                        admissionNo
                        );
                    }

                    if (rollNumber == null ||
                            rollNumber.isBlank()) {

                        throw new IllegalArgumentException(
                                "Excel Roll Number is empty for Admission No: " +
                                        admissionNo
                        );
                    }

                    if (!student.getRollNumber()
                            .trim()
                            .equalsIgnoreCase(
                                    rollNumber.trim()
                            )) {

                        throw new IllegalArgumentException(
                                "Roll Number mismatch. Admission No=" +
                                        admissionNo +
                                        ", DB=" +
                                        student.getRollNumber() +
                                        ", Excel=" +
                                        rollNumber
                        );
                    }

                    System.out.println(
                            "ROLL NUMBER VALIDATION PASSED"
                    );

                    // -------------------------------------------------
                    // CLASS VALIDATION
                    // -------------------------------------------------

                    if (student.getSection() == null ||
                            student.getSection()
                                    .getClassEntity() == null ||
                            !student.getSection()
                                    .getClassEntity()
                                    .getId()
                                    .equals(clazz.getId())) {

                        throw new IllegalArgumentException(
                                "Student does not belong to selected class"
                        );
                    }

                    System.out.println(
                            "STUDENT CLASS VALIDATION PASSED"
                    );

                    // -------------------------------------------------
                    // SECTION VALIDATION
                    // -------------------------------------------------

                    if (section != null &&
                            student.getSection() != null &&
                            !student.getSection()
                                    .getId()
                                    .equals(section.getId())) {

                        throw new IllegalArgumentException(
                                "Student does not belong to selected section"
                        );
                    }

                    System.out.println(
                            "STUDENT SECTION VALIDATION PASSED"
                    );

                    // -------------------------------------------------
                    // MARKS VALIDATION
                    // -------------------------------------------------

                    if (marksObtained < 0) {

                        throw new IllegalArgumentException(
                                "Marks cannot be negative"
                        );
                    }

                    if (schedule.getMaxMarks() != null &&
                            marksObtained >
                                    schedule.getMaxMarks()) {

                        throw new IllegalArgumentException(
                                "Marks cannot exceed maximum marks: " +
                                        schedule.getMaxMarks()
                        );
                    }

                    // -------------------------------------------------
                    // EXISTING RESULT
                    // -------------------------------------------------

                    System.out.println(
                            "CHECKING EXISTING RESULT..."
                    );

                    ExamResult result =
                            resultRepository
                                    .findByExamScheduleIdAndStudentId(
                                            schedule.getId(),
                                            student.getId()
                                    )
                                    .orElse(null);

                    if (result == null) {

                        System.out.println(
                                "NO EXISTING RESULT -> CREATE"
                        );

                        result =
                                ExamResult.builder()
                                        .examSchedule(schedule)
                                        .student(student)
                                        .build();

                        created++;

                    } else {

                        System.out.println(
                                "EXISTING RESULT FOUND: ID=" +
                                        result.getId() +
                                        " -> UPDATE"
                        );

                        updated++;
                    }

                    // -------------------------------------------------
                    // SAVE RESULT
                    // -------------------------------------------------

                    result.setMarkedByTeacher(
                            schedule.getSubjectTeacher()
                    );

                    result.setMarksObtained(
                            marksObtained
                    );

                    result.setMaxMarks(
                            schedule.getMaxMarks()
                    );

                    if (schedule.getMaxMarks() != null &&
                            schedule.getMaxMarks() > 0) {

                        double percentage =
                                marksObtained * 100.0 /
                                        schedule.getMaxMarks();

                        result.setPercentage(
                                percentage
                        );

                        result.setGrade(
                                gradeFor(percentage)
                        );

                        System.out.println(
                                "PERCENTAGE = " +
                                        percentage
                        );

                        System.out.println(
                                "GRADE = " +
                                        result.getGrade()
                        );

                    } else {

                        result.setPercentage(null);
                        result.setGrade(null);
                    }

                    result.setRemarks(remarks);

                    result.setStatus("PUBLISHED");

                    result.setIsPublished(true);

                    result.setPublishedAt(
                            LocalDateTime.now()
                    );

                    result.setMarkedAt(
                            LocalDateTime.now()
                    );

                    System.out.println(
                            "SAVING RESULT..."
                    );

                    resultRepository.save(result);

                    System.out.println(
                            "RESULT SAVED SUCCESSFULLY"
                    );

                    System.out.println(
                            "RESULT ID = " +
                                    result.getId()
                    );

                    affectedScheduleIds.add(
                            schedule.getId()
                    );

                    System.out.println(
                            "ROW PROCESSED SUCCESSFULLY"
                    );

                } catch (Exception e) {

                    errorCount++;

                    String errorMessage =
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Unknown error";

                    errors.add(
                            ExamResultImportErrorDto.builder()
                                    .row(i + 1)
                                    .message(errorMessage)
                                    .build()
                    );

                    System.out.println();
                    System.out.println(
                            "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
                    );

                    System.out.println(
                            "ERROR PROCESSING ROW: " +
                                    (i + 1)
                    );

                    System.out.println(
                            "ERROR MESSAGE: " +
                                    errorMessage
                    );

                    e.printStackTrace();

                    System.out.println(
                            "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
                    );
                }
            }

            // ---------------------------------------------------------
            // RANKS
            // ---------------------------------------------------------

            System.out.println();
            System.out.println(
                    "=================================================="
            );

            System.out.println(
                    "RECALCULATING RANKS"
            );

            System.out.println(
                    "AFFECTED SCHEDULE IDS: " +
                            affectedScheduleIds
            );

            for (Long scheduleId :
                    affectedScheduleIds) {

                System.out.println(
                        "RECALCULATING SCHEDULE: " +
                                scheduleId
                );

                recalculateRanks(scheduleId);
            }

        } catch (Exception e) {

            System.out.println();
            System.out.println(
                    "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
            );

            System.out.println(
                    "FAILED TO READ EXAM RESULT EXCEL"
            );

            System.out.println(
                    "ERROR: " +
                            e.getMessage()
            );

            e.printStackTrace();

            System.out.println(
                    "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
            );

            throw new RuntimeException(
                    "Failed to read Exam Result Excel file",
                    e
            );
        }

        // ---------------------------------------------------------
        // FINAL SUMMARY
        // ---------------------------------------------------------

        System.out.println();
        System.out.println(
                "=================================================="
        );

        System.out.println(
                "          EXAM RESULT IMPORT FINISHED"
        );

        System.out.println(
                "TOTAL ROWS : " +
                        totalRows
        );

        System.out.println(
                "CREATED    : " +
                        created
        );

        System.out.println(
                "UPDATED    : " +
                        updated
        );

        System.out.println(
                "ERRORS     : " +
                        errorCount
        );

        System.out.println(
                "ERROR DETAILS:"
        );

        for (ExamResultImportErrorDto error : errors) {
            System.out.println(
                    "  -> Row " +
                            error.getRow() +
                            ": " +
                            error.getMessage()
            );
        }
        System.out.println(
                "=================================================="
        );

        return ExamResultImportResponseDto.builder()
                .totalRows(totalRows)
                .created(created)
                .updated(updated)
                .errors(errorCount)
                .errorDetails(errors)
                .build();
    }
    // =========================================================
    // GRADE
    // =========================================================

    private String gradeFor(Double percentage) {
        if (percentage == null) {
            return null;
        }

        List<GradeRule> rules =
                gradeRuleRepository
                        .findAllByOrderByMinimumPercentageDesc();

        for (GradeRule rule : rules) {

            if (rule.getMinimumPercentage() != null
                    && percentage >= rule.getMinimumPercentage()) {

                return rule.getGrade();
            }
        }

        return null;
    }

    // =========================================================
    // RANKS
    // =========================================================

    private void recalculateRanks(
            Long scheduleId) {

        List<ExamResult> results =
                resultRepository
                        .findByExamScheduleIdOrderByPercentageDesc(
                                scheduleId
                        );

        int rank = 0;
        Double previousPercentage = null;

        for (int i = 0;
             i < results.size();
             i++) {

            ExamResult result =
                    results.get(i);

            Double percentage =
                    result.getPercentage();

            if (percentage == null) {

                result.setClassRank(null);
                result.setSectionRank(null);

                resultRepository.save(result);

                continue;
            }


            if (previousPercentage == null ||
                    Double.compare(
                            previousPercentage,
                            percentage
                    ) != 0) {

                rank = i + 1;
            }

            result.setClassRank(rank);
            result.setSectionRank(rank);

            previousPercentage = percentage;

            resultRepository.save(result);
        }
    }


    // =========================================================
    // STRING
    // =========================================================

    private String getCellString(Cell cell) {

        if (cell == null) {
            return null;
        }

        DataFormatter formatter =
                new DataFormatter();

        String value =
                formatter.formatCellValue(cell);

        if (value == null ||
                value.trim().isEmpty()) {

            return null;
        }

        return value.trim();
    }


    // =========================================================
    // INTEGER
    // =========================================================

    private Integer getCellInteger(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() ==
                CellType.NUMERIC) {

            return (int)
                    cell.getNumericCellValue();
        }

        String value =
                getCellString(cell);

        if (value == null) {
            return null;
        }

        try {

            return Integer.parseInt(value);

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Invalid number: " + value
            );
        }
    }


    // =========================================================
    // DATE
    // =========================================================

    private LocalDate getLocalDate(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() ==
                CellType.NUMERIC &&
                DateUtil.isCellDateFormatted(cell)) {

            return cell
                    .getLocalDateTimeCellValue()
                    .toLocalDate();
        }

        String value =
                getCellString(cell);

        if (value == null) {
            return null;
        }

        List<DateTimeFormatter> formatters =
                List.of(
                        DateTimeFormatter.ofPattern(
                                "yyyy-MM-dd"
                        ),
                        DateTimeFormatter.ofPattern(
                                "dd-MM-yyyy"
                        ),
                        DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy"
                        )
                );

        for (DateTimeFormatter formatter :
                formatters) {

            try {

                return LocalDate.parse(
                        value,
                        formatter
                );

            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException(
                "Invalid date: " + value
        );
    }


    // =========================================================
    // TIME
    // =========================================================

    private LocalTime getLocalTime(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() ==
                CellType.NUMERIC) {

            double value =
                    cell.getNumericCellValue();

            int totalSeconds =
                    (int) Math.round(
                            value * 24 * 60 * 60
                    );

            int hours =
                    totalSeconds / 3600;

            int minutes =
                    (totalSeconds % 3600) / 60;

            int seconds =
                    totalSeconds % 60;

            return LocalTime.of(
                    hours,
                    minutes,
                    seconds
            );
        }

        String value =
                getCellString(cell);

        if (value == null) {
            return null;
        }

        List<DateTimeFormatter> formatters =
                List.of(
                        DateTimeFormatter.ofPattern(
                                "HH:mm"
                        ),
                        DateTimeFormatter.ofPattern(
                                "HH:mm:ss"
                        )
                );

        for (DateTimeFormatter formatter :
                formatters) {

            try {

                return LocalTime.parse(
                        value,
                        formatter
                );

            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException(
                "Invalid time: " + value
        );
    }


    // =========================================================
    // EMPTY ROW
    // =========================================================

    private boolean isRowEmpty(Row row) {

        for (int i = row.getFirstCellNum();
             i < row.getLastCellNum();
             i++) {

            Cell cell = row.getCell(i);

            if (cell != null &&
                    cell.getCellType() !=
                            CellType.BLANK &&
                    getCellString(cell) != null) {

                return false;
            }
        }

        return true;
    }
}