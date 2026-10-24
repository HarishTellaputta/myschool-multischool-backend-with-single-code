package com.harish.student_management.service;

import com.harish.student_management.dto.ClassImportResponseDto;
import com.harish.student_management.dto.ClassRequestDto;
import com.harish.student_management.dto.ClassResponseDto;
import com.harish.student_management.entity.ClassEntity;
import com.harish.student_management.entity.School;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.ClassRepository;
import com.harish.student_management.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private static final Logger logger =
            LoggerFactory.getLogger(ClassServiceImpl.class);

    private final ClassRepository classRepository;
    private final SchoolRepository schoolRepository;

    private final CurrentSchoolService currentSchoolService;


    // =========================================================
    // CREATE
    // =========================================================

    @Override
    public ClassResponseDto createClass(ClassRequestDto request) {

        logger.info(
                "Creating class with code: {}",
                request.getCode()
        );

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "School not found with id: " + schoolId
                        )
                );

        if (classRepository.existsBySchoolIdAndCode(
                schoolId, request.getCode())) {
            throw new IllegalArgumentException(
                    "Class already exists with code: " + request.getCode()
            );
        }

        ClassEntity classEntity = ClassEntity.builder()
                .school(school)
                .name(request.getName())
                .code(request.getCode())
                .grade(request.getGrade())
                .year(request.getYear())
                .description(request.getDescription())
                .build();

        ClassEntity savedClass =
                classRepository.save(classEntity);

        logger.info(
                "Class created successfully with ID: {}",
                savedClass.getId()
        );

        return mapToResponseDto(savedClass);
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @Override
    public ClassResponseDto getClassById(Long id) {

        logger.info(
                "Fetching class with ID: {}",
                id
        );

        ClassEntity classEntity =
                classRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Class not found with id: "
                                                + id
                                )
                        );

        Long currentSchoolId = currentSchoolService.getCurrentSchoolId();

        if (classEntity.getSchool() == null
                || !currentSchoolId.equals(classEntity.getSchool().getId())) {
            throw new ResourceNotFoundException(
                    "Class not found with id: " + id
            );
        }

        return mapToResponseDto(classEntity);
    }

    // =========================================================
    // GET ALL
    // =========================================================


    @Override
    public List<ClassResponseDto> getAllClasses() {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return classRepository
                .findBySchoolIdWithSchool(schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }


    // =========================================================
    // GET BY SCHOOL
    // =========================================================


    @Override
    public List<ClassResponseDto> getClassesBySchoolId(Long schoolId) {

        Long currentSchoolId =
                currentSchoolService.getCurrentSchoolId();

        if (!currentSchoolId.equals(schoolId)) {
            throw new ResourceNotFoundException(
                    "School not found with id: " + schoolId
            );
        }

        return classRepository
                .findBySchoolIdWithSchool(currentSchoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    public ClassResponseDto updateClass(
            Long id,
            ClassRequestDto request) {

        logger.info(
                "Updating class with ID: {}",
                id
        );

        ClassEntity classEntity =
                classRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Class not found with id: "
                                                + id
                                )
                        );

        if (!classEntity.getCode().equals(request.getCode())
                && classRepository.existsByCode(request.getCode())) {

            throw new IllegalArgumentException(
                    "Class already exists with code: "
                            + request.getCode()
            );
        }

        School school =
                schoolRepository.findById(request.getSchoolId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "School not found with id: "
                                                + request.getSchoolId()
                                )
                        );

        classEntity.setSchool(school);
        classEntity.setName(request.getName());
        classEntity.setCode(request.getCode());
        classEntity.setGrade(request.getGrade());
        classEntity.setYear(request.getYear());
        classEntity.setDescription(request.getDescription());

        ClassEntity updatedClass =
                classRepository.save(classEntity);

        logger.info(
                "Class updated successfully with ID: {}",
                updatedClass.getId()
        );

        return mapToResponseDto(updatedClass);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Override
    public void deleteClass(Long id) {

        logger.info(
                "Deleting class with ID: {}",
                id
        );

        ClassEntity classEntity =
                classRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Class not found with id: "
                                                + id
                                )
                        );

        classRepository.delete(classEntity);

        logger.info(
                "Class deleted successfully with ID: {}",
                id
        );
    }

    // =========================================================
    // EXCEL IMPORT
    // =========================================================

    @Override
    public ClassImportResponseDto importClasses(
            MultipartFile file) throws IOException {

        Long currentSchoolId = currentSchoolService.getCurrentSchoolId();
        School school = schoolRepository.findById(currentSchoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "School not found with id: " + currentSchoolId
                        )
                );

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Excel file is empty."
            );
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null
                || !fileName.toLowerCase().endsWith(".xlsx")) {

            throw new IllegalArgumentException(
                    "Only .xlsx Excel files are supported."
            );
        }

        // -----------------------------------------------------
        // Verify school
        // -----------------------------------------------------

//        School school = schoolRepository.findById(schoolId)
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "School not found with id: "
//                                        + schoolId
//                        )
//                );

        int totalRows = 0;
        int createdCount = 0;
        int updatedCount = 0;
        int failedCount = 0;

        List<String> errors = new ArrayList<>();

        // Track codes inside the uploaded file
        Set<String> processedCodes =
                new HashSet<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook =
                     new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() <= 1) {

                throw new IllegalArgumentException(
                        "Excel file does not contain any class data."
                );
            }

            DataFormatter formatter =
                    new DataFormatter();

            // -------------------------------------------------
            // HEADER VALIDATION
            // -------------------------------------------------

            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalArgumentException(
                        "Excel header row is missing."
                );
            }

            String[] requiredHeaders = {
                    "code",
                    "name",
                    "grade",
                    "description",
                    "year"
            };

            for (int i = 0;
                 i < requiredHeaders.length;
                 i++) {

                String actualHeader =
                        formatter.formatCellValue(
                                headerRow.getCell(i)
                        ).trim();

                if (!requiredHeaders[i]
                        .equalsIgnoreCase(actualHeader)) {

                    throw new IllegalArgumentException(
                            "Invalid Excel format. Expected column "
                                    + (i + 1)
                                    + " to be: "
                                    + requiredHeaders[i]
                    );
                }
            }

            // -------------------------------------------------
            // PROCESS ROWS
            // -------------------------------------------------

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (row == null) {
                    continue;
                }

                // Check completely empty row
                boolean emptyRow = true;

                for (int i = 0; i < 5; i++) {

                    String value =
                            formatter.formatCellValue(
                                    row.getCell(i)
                            ).trim();

                    if (!value.isEmpty()) {
                        emptyRow = false;
                        break;
                    }
                }

                if (emptyRow) {
                    continue;
                }

                totalRows++;

                int excelRowNumber = rowIndex + 1;

                try {

                    // -----------------------------------------
                    // READ VALUES
                    // -----------------------------------------

                    String code =
                            formatter.formatCellValue(
                                    row.getCell(0)
                            ).trim();

                    String name =
                            formatter.formatCellValue(
                                    row.getCell(1)
                            ).trim();

                    String grade =
                            formatter.formatCellValue(
                                    row.getCell(2)
                            ).trim();

                    String description =
                            formatter.formatCellValue(
                                    row.getCell(3)
                            ).trim();

                    String yearValue =
                            formatter.formatCellValue(
                                    row.getCell(4)
                            ).trim();

                    // -----------------------------------------
                    // VALIDATION
                    // -----------------------------------------

                    if (code.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Code is required."
                        );
                    }

                    if (name.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Name is required."
                        );
                    }

                    if (grade.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Grade is required."
                        );
                    }

                    if (yearValue.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Year is required."
                        );
                    }

                    Integer year;

                    try {

                        double numericYear =
                                Double.parseDouble(yearValue);

                        year = (int) numericYear;

                    } catch (NumberFormatException e) {

                        throw new IllegalArgumentException(
                                "Year must be a valid number."
                        );
                    }

                    // -----------------------------------------
                    // DUPLICATE INSIDE SAME EXCEL
                    // -----------------------------------------

                    String normalizedCode =
                            code.toUpperCase(Locale.ROOT);

                    if (!processedCodes.add(normalizedCode)) {

                        throw new IllegalArgumentException(
                                "Duplicate class code in Excel: "
                                        + code
                        );
                    }

                    // -----------------------------------------
                    // FIND EXISTING CLASS
                    // -----------------------------------------
// -----------------------------------------
// FIND EXISTING CLASS — SCHOOL-WISE
// -----------------------------------------

                    Optional<ClassEntity> existingClassOptional =
                            classRepository.findBySchoolIdAndCode(
                                    currentSchoolId,
                                    code
                            );

                    if (existingClassOptional.isPresent()) {

                        ClassEntity existingClass = existingClassOptional.get();

                        existingClass.setName(name);
                        existingClass.setGrade(grade);
                        existingClass.setDescription(
                                description.isEmpty() ? null : description
                        );
                        existingClass.setYear(year);

                        classRepository.save(existingClass);
                        updatedCount++;

                    } else {

                        ClassEntity newClass = ClassEntity.builder()
                                .school(school)
                                .code(code)
                                .name(name)
                                .grade(grade)
                                .description(
                                        description.isEmpty() ? null : description
                                )
                                .year(year)
                                .build();

                        classRepository.save(newClass);
                        createdCount++;
                    }

                } catch (Exception e) {

                    failedCount++;

                    errors.add(
                            "Row "
                                    + excelRowNumber
                                    + ": "
                                    + e.getMessage()
                    );

                    logger.warn(
                            "Failed to import class at row {}: {}",
                            excelRowNumber,
                            e.getMessage()
                    );
                }
            }
        }

        logger.info(
                "Class Excel import completed. Total: {}, Created: {}, Updated: {}, Failed: {}",
                totalRows,
                createdCount,
                updatedCount,
                failedCount
        );

        return ClassImportResponseDto.builder()
                .totalRows(totalRows)
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .failedCount(failedCount)
                .errors(errors)
                .message(
                        failedCount == 0
                                ? "Classes Excel imported successfully."
                                : "Classes Excel imported with some errors."
                )
                .build();
    }

    // =========================================================
    // RESPONSE MAPPER
    // =========================================================

    private ClassResponseDto mapToResponseDto(
            ClassEntity classEntity) {

        return ClassResponseDto.builder()
                .id(classEntity.getId())
                .schoolId(
                        classEntity.getSchool() != null
                                ? classEntity.getSchool().getId()
                                : null
                )
                .schoolName(
                        classEntity.getSchool() != null
                                ? classEntity.getSchool().getName()
                                : null
                )
                .name(classEntity.getName())
                .code(classEntity.getCode())
                .grade(classEntity.getGrade())
                .year(classEntity.getYear())
                .description(classEntity.getDescription())
                .createdAt(classEntity.getCreatedAt())
                .updatedAt(classEntity.getUpdatedAt())
                .build();
    }
}