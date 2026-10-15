
        package com.harish.student_management.service;

import com.harish.student_management.entity.ClassEntity;
import com.harish.student_management.entity.McqQuestion;
import com.harish.student_management.entity.Subject;
import com.harish.student_management.repository.ClassRepository;
import com.harish.student_management.repository.ClassSubjectRepository;
import com.harish.student_management.repository.McqQuestionRepository;
import com.harish.student_management.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class McqExcelImportService {

    private final McqQuestionRepository mcqQuestionRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;

    // ============================================================
    // IMPORT EXCEL
    // ============================================================

    @Transactional
    public int importExcel(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Excel file is empty"
            );
        }

        int imported = 0;

        try (
                InputStream inputStream = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(inputStream)
        ) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException(
                        "Excel file contains no sheets"
                );
            }

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() <= 1) {
                throw new IllegalArgumentException(
                        "Excel has no MCQ data"
                );
            }

            Row header = sheet.getRow(0);

            if (header == null) {
                throw new IllegalArgumentException(
                        "Excel header row is missing"
                );
            }

            Map<String, Integer> columns =
                    readHeaders(header);

            validateHeaders(columns);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null || isEmpty(row)) {
                    continue;
                }

                int excelRowNumber = i + 1;

                // ====================================================
                // BASIC VALUES
                // ====================================================

                String subjectName =
                        value(
                                row,
                                columns,
                                "Subject"
                        );

                String className =
                        value(
                                row,
                                columns,
                                "Class"
                        );

                String questionText =
                        value(
                                row,
                                columns,
                                "Question"
                        );

                if (subjectName.isBlank()) {
                    throw new IllegalArgumentException(
                            "Subject missing at Excel row "
                                    + excelRowNumber
                    );
                }

                if (className.isBlank()) {
                    throw new IllegalArgumentException(
                            "Class missing at Excel row "
                                    + excelRowNumber
                    );
                }

                if (questionText.isBlank()) {
                    throw new IllegalArgumentException(
                            "Question missing at Excel row "
                                    + excelRowNumber
                    );
                }

                // ====================================================
                // FIND CLASS
                // ====================================================

                ClassEntity classEntity =
                        findClass(className);

                // ====================================================
                // FIND SUBJECT
                // ====================================================

                Subject subject =
                        findSubject(
                                subjectName,
                                classEntity
                        );

                // ====================================================
                // CREATE MCQ
                // ====================================================

                McqQuestion question =
                        new McqQuestion();

                question.setSubject(subject);

                question.setClassEntity(
                        classEntity
                );

                question.setQuestion(
                        questionText.trim()
                );

                // ====================================================
                // QUESTION IMAGE
                // ====================================================

                question.setQuestionImageUrl(
                        imageValue(
                                row,
                                columns,
                                "Question Image"
                        )
                );

                // ====================================================
                // OPTION A
                // ====================================================

                question.setOptionA(
                        required(
                                row,
                                columns,
                                "Option A",
                                i
                        )
                );

                question.setOptionAImageUrl(
                        imageValue(
                                row,
                                columns,
                                "Option A Image"
                        )
                );

                // ====================================================
                // OPTION B
                // ====================================================

                question.setOptionB(
                        required(
                                row,
                                columns,
                                "Option B",
                                i
                        )
                );

                question.setOptionBImageUrl(
                        imageValue(
                                row,
                                columns,
                                "Option B Image"
                        )
                );

                // ====================================================
                // OPTION C
                // ====================================================

                question.setOptionC(
                        required(
                                row,
                                columns,
                                "Option C",
                                i
                        )
                );

                question.setOptionCImageUrl(
                        imageValue(
                                row,
                                columns,
                                "Option C Image"
                        )
                );

                // ====================================================
                // OPTION D
                // ====================================================

                question.setOptionD(
                        required(
                                row,
                                columns,
                                "Option D",
                                i
                        )
                );

                question.setOptionDImageUrl(
                        imageValue(
                                row,
                                columns,
                                "Option D Image"
                        )
                );

                // ====================================================
                // CORRECT ANSWER
                // ====================================================

                String correctAnswer =
                        required(
                                row,
                                columns,
                                "Correct Answer",
                                i
                        )
                                .trim()
                                .toUpperCase();

                if (!correctAnswer.matches("[ABCD]")) {

                    throw new IllegalArgumentException(
                            "Correct Answer must be A/B/C/D at row "
                                    + excelRowNumber
                    );
                }

                question.setCorrectAnswer(
                        correctAnswer
                );

                // ====================================================
                // MARKS
                // ====================================================

                String marks =
                        value(
                                row,
                                columns,
                                "Marks"
                        );

                if (marks.isBlank()) {

                    throw new IllegalArgumentException(
                            "Marks missing at row "
                                    + excelRowNumber
                    );
                }

                try {

                    int marksValue =
                            Integer.parseInt(
                                    marks.trim()
                            );

                    if (marksValue <= 0) {

                        throw new IllegalArgumentException(
                                "Marks must be greater than zero at row "
                                        + excelRowNumber
                        );
                    }

                    question.setMarks(
                            marksValue
                    );

                } catch (NumberFormatException e) {

                    throw new IllegalArgumentException(
                            "Invalid Marks value '"
                                    + marks
                                    + "' at row "
                                    + excelRowNumber
                    );
                }

                // ====================================================
                // EXPLANATION
                // ====================================================

                String explanation =
                        value(
                                row,
                                columns,
                                "Explanation"
                        );

                question.setExplanation(
                        explanation.isBlank()
                                ? null
                                : explanation.trim()
                );

                // ====================================================
                // QUESTION DATE
                // ====================================================

                String date =
                        value(
                                row,
                                columns,
                                "Question Date"
                        );

                if (date.isBlank()) {

                    question.setQuestionDate(
                            LocalDate.now()
                    );

                } else {

                    question.setQuestionDate(
                            parseDate(
                                    row,
                                    columns,
                                    "Question Date"
                            )
                    );
                }

                // ====================================================
                // SAVE
                // ====================================================

                mcqQuestionRepository.save(
                        question
                );

                imported++;
            }

            return imported;

        } catch (Exception e) {

            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }

            throw new RuntimeException(
                    "MCQ Excel import failed: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // ============================================================
    // READ HEADERS
    // ============================================================

    private Map<String, Integer> readHeaders(
            Row header
    ) {

        Map<String, Integer> columns =
                new HashMap<>();

        for (Cell cell : header) {

            String name =
                    new DataFormatter()
                            .formatCellValue(cell)
                            .trim();

            if (!name.isBlank()) {

                columns.put(
                        normalize(name),
                        cell.getColumnIndex()
                );
            }
        }

        return columns;
    }

    // ============================================================
    // VALIDATE HEADERS
    // ============================================================

    private void validateHeaders(
            Map<String, Integer> columns
    ) {

        String[] required = {

                "Subject",

                "Class",

                "Question",

                "Option A",

                "Option B",

                "Option C",

                "Option D",

                "Correct Answer",

                "Marks"
        };

        for (String column : required) {

            if (!columns.containsKey(
                    normalize(column)
            )) {

                throw new IllegalArgumentException(
                        "Missing Excel column: "
                                + column
                );
            }
        }
    }

    // ============================================================
    // FIND CLASS
    // ============================================================

    private ClassEntity findClass(
            String className
    ) {

        String searchName =
                className.trim();

        return classRepository
                .findAll()
                .stream()
                .filter(c ->
                        c.getName() != null
                                && c.getName()
                                .trim()
                                .equalsIgnoreCase(
                                        searchName
                                )
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Class not found: "
                                        + className
                        )
                );
    }

    // ============================================================
    // FIND SUBJECT
    // ============================================================

    private Subject findSubject(
            String subjectName,
            ClassEntity classEntity
    ) {

        String searchName =
                subjectName.trim();

        Subject subject =
                subjectRepository
                        .findAll()
                        .stream()
                        .filter(s ->
                                s.getName() != null
                                        && s.getName()
                                        .trim()
                                        .equalsIgnoreCase(
                                                searchName
                                        )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Subject not found: "
                                                + subjectName
                                )
                        );

        // ========================================================
        // SCHOOL VALIDATION
        // ========================================================

        if (subject.getSchool() == null) {

            throw new IllegalArgumentException(
                    "Subject '"
                            + subjectName
                            + "' is not associated with a school"
            );
        }

        if (classEntity.getSchool() == null) {

            throw new IllegalArgumentException(
                    "Class '"
                            + classEntity.getName()
                            + "' is not associated with a school"
            );
        }

        if (!subject.getSchool()
                .getId()
                .equals(
                        classEntity
                                .getSchool()
                                .getId()
                )) {

            throw new IllegalArgumentException(
                    "Subject '"
                            + subjectName
                            + "' does not belong to the same school as class '"
                            + classEntity.getName()
                            + "'"
            );
        }

        // ========================================================
        // CLASS ↔ SUBJECT VALIDATION
        // ========================================================

        boolean assignedToClass =
                classSubjectRepository
                        .existsByClassEntityIdAndSubjectId(
                                classEntity.getId(),
                                subject.getId()
                        );

        if (!assignedToClass) {

            throw new IllegalArgumentException(
                    "Subject '"
                            + subjectName
                            + "' is not assigned to class '"
                            + classEntity.getName()
                            + "'"
            );
        }

        return subject;
    }

    // ============================================================
    // REQUIRED VALUE
    // ============================================================

    private String required(
            Row row,
            Map<String, Integer> columns,
            String column,
            int rowNumber
    ) {

        String value =
                value(
                        row,
                        columns,
                        column
                );

        if (value.isBlank()) {

            throw new IllegalArgumentException(
                    column
                            + " missing at row "
                            + (rowNumber + 1)
            );
        }

        return value.trim();
    }

    // ============================================================
    // READ CELL VALUE
    // ============================================================

    private String value(
            Row row,
            Map<String, Integer> columns,
            String column
    ) {

        Integer index =
                columns.get(
                        normalize(column)
                );

        if (index == null) {
            return "";
        }

        Cell cell =
                row.getCell(index);

        if (cell == null) {
            return "";
        }

        DataFormatter formatter =
                new DataFormatter();

        return formatter
                .formatCellValue(cell)
                .trim();
    }

    // ============================================================
    // IMAGE VALUE
    // ============================================================

    private String imageValue(
            Row row,
            Map<String, Integer> columns,
            String column
    ) {

        String value =
                value(
                        row,
                        columns,
                        column
                );

        if (value.isBlank()) {
            return null;
        }

        // Already a complete/relative upload path
        if (value.startsWith("/")) {
            return value;
        }

        // Filename supplied from Excel
        return "/uploads/mcq/" + value;
    }

    // ============================================================
    // DATE PARSER
    // ============================================================

    private LocalDate parseDate(
            Row row,
            Map<String, Integer> columns,
            String column
    ) {

        Integer index =
                columns.get(
                        normalize(column)
                );

        if (index == null) {
            return LocalDate.now();
        }

        Cell cell =
                row.getCell(index);

        if (cell == null) {
            return LocalDate.now();
        }

        // ========================================================
        // EXCEL NATIVE DATE
        // ========================================================

        if (cell.getCellType()
                == CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {

            return cell
                    .getLocalDateTimeCellValue()
                    .toLocalDate();
        }

        String value =
                new DataFormatter()
                        .formatCellValue(cell)
                        .trim();

        if (value.isBlank()) {
            return LocalDate.now();
        }

        // ========================================================
        // yyyy-MM-dd
        // ========================================================

        try {

            return LocalDate.parse(
                    value
            );

        } catch (Exception ignored) {
        }

        // ========================================================
        // dd-MM-yyyy
        // ========================================================

        try {

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "dd-MM-yyyy"
                    );

            return LocalDate.parse(
                    value,
                    formatter
            );

        } catch (Exception ignored) {
        }

        // ========================================================
        // dd/MM/yyyy
        // ========================================================

        try {

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "dd/MM/yyyy"
                    );

            return LocalDate.parse(
                    value,
                    formatter
            );

        } catch (Exception ignored) {
        }

        throw new IllegalArgumentException(
                "Invalid Question Date: "
                        + value
                        + ". Use dd-MM-yyyy or yyyy-MM-dd"
        );
    }

    // ============================================================
    // CHECK EMPTY ROW
    // ============================================================

    private boolean isEmpty(
            Row row
    ) {

        DataFormatter formatter =
                new DataFormatter();

        for (Cell cell : row) {

            if (cell != null
                    && !formatter
                    .formatCellValue(cell)
                    .trim()
                    .isBlank()) {

                return false;
            }
        }

        return true;
    }

    // ============================================================
    // NORMALIZE HEADER
    // ============================================================

    private String normalize(
            String value
    ) {

        return value
                .trim()
                .toLowerCase()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }
}

