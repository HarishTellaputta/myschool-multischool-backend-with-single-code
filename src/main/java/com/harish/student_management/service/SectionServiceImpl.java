package com.harish.student_management.service;

import com.harish.student_management.dto.SectionImportResponseDto;
import com.harish.student_management.dto.SectionRequestDto;
import com.harish.student_management.dto.SectionResponseDto;
import com.harish.student_management.entity.ClassEntity;
import com.harish.student_management.entity.Section;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.ClassRepository;
import com.harish.student_management.repository.SectionRepository;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SectionServiceImpl implements SectionService {

    private static final Logger logger =
            LoggerFactory.getLogger(SectionServiceImpl.class);

    private final SectionRepository sectionRepository;
    private final ClassRepository classRepository;


    private final CurrentSchoolService currentSchoolService;

    private Long getSchoolId() {
        return currentSchoolService.getCurrentSchoolId();
    }

    private ClassEntity getClassForCurrentSchool(Long classId) {
        return classRepository
                .findByIdAndSchool_Id(classId, getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found with id: " + classId
                ));
    }

    private Section getSectionForCurrentSchool(Long sectionId) {
        return sectionRepository
                .findByIdAndSchoolId(sectionId, getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found with id: " + sectionId
                ));
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    public SectionResponseDto createSection(SectionRequestDto request) {
        logger.info("Creating section with name: {}", request.getName());

        // Only a class belonging to the authenticated user's school
        // can be assigned to this section.
        ClassEntity classEntity =
                getClassForCurrentSchool(request.getClassId());

        Section section = Section.builder()
                .classEntity(classEntity)
                .name(request.getName())
                .capacity(request.getCapacity())
                .description(request.getDescription())
                .build();

        Section savedSection = sectionRepository.save(section);

        logger.info(
                "Section created successfully with ID: {}",
                savedSection.getId()
        );

        return mapToResponseDto(savedSection);
    }
    // =========================================================
    // GET BY ID
    // =========================================================

    @Override
    public SectionResponseDto getSectionById(Long id) {
        logger.info("Fetching section with ID: {}", id);

        Section section = getSectionForCurrentSchool(id);

        return mapToResponseDto(section);
    }
    // =========================================================
    // GET ALL
    // =========================================================

    @Override
    public List<SectionResponseDto> getAllSections() {
        Long schoolId = getSchoolId();

        logger.info("Fetching all sections for school ID: {}", schoolId);

        return sectionRepository.findAllBySchoolId(schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }
    // =========================================================
    // GET BY CLASS
    // =========================================================

    @Override
    public List<SectionResponseDto> getSectionsByClassId(Long classId) {
        Long schoolId = getSchoolId();

        logger.info(
                "Fetching sections for class ID: {} and school ID: {}",
                classId,
                schoolId
        );

        // Reject class IDs belonging to another school.
        getClassForCurrentSchool(classId);

        return sectionRepository
                .findByClassIdAndSchoolId(classId, schoolId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }
    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    public SectionResponseDto updateSection(
            Long id,
            SectionRequestDto request) {

        logger.info("Updating section with ID: {}", id);

        // Section must belong to the authenticated user's school.
        Section section = getSectionForCurrentSchool(id);

        // Destination class must also belong to that same school.
        ClassEntity classEntity =
                getClassForCurrentSchool(request.getClassId());

        section.setClassEntity(classEntity);
        section.setName(request.getName());
        section.setCapacity(request.getCapacity());
        section.setDescription(request.getDescription());

        Section updatedSection = sectionRepository.save(section);

        logger.info(
                "Section updated successfully with ID: {}",
                updatedSection.getId()
        );

        return mapToResponseDto(updatedSection);
    }
    // =========================================================
    // DELETE
    // =========================================================

    @Override
    public void deleteSection(Long id) {
        logger.info("Deleting section with ID: {}", id);

        Section section = getSectionForCurrentSchool(id);

        sectionRepository.delete(section);

        logger.info("Section deleted successfully with ID: {}", id);
    }
    // =========================================================
    // EXCEL IMPORT
    // =========================================================

    @Override
    public SectionImportResponseDto importSections(
            MultipartFile file) throws IOException {

        // Never accept schoolId from the client.
        Long schoolId = getSchoolId();

        logger.info(
                "Starting section Excel import for school ID: {}",
                schoolId
        );

        // Keep the rest of your existing Excel import logic below this.

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Excel file is empty."
            );
        }

        String fileName =
                file.getOriginalFilename();

        if (fileName == null
                || !fileName
                .toLowerCase(Locale.ROOT)
                .endsWith(".xlsx")) {

            throw new IllegalArgumentException(
                    "Only .xlsx Excel files are supported."
            );
        }

        int totalRows = 0;
        int createdCount = 0;
        int updatedCount = 0;
        int failedCount = 0;

        List<String> errors =
                new ArrayList<>();

        /*
         * Prevent duplicate:
         *
         * 1st Class + A
         * 1st Class + A
         *
         * inside the same Excel file.
         */
        Set<String> processedSections =
                new HashSet<>();

        try (InputStream inputStream =
                     file.getInputStream();
             Workbook workbook =
                     new XSSFWorkbook(inputStream)) {

            Sheet sheet =
                    workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() <= 1) {

                throw new IllegalArgumentException(
                        "Excel file does not contain any section data."
                );
            }

            DataFormatter formatter =
                    new DataFormatter();

            // -------------------------------------------------
            // HEADER VALIDATION
            // -------------------------------------------------

            Row headerRow =
                    sheet.getRow(0);

            if (headerRow == null) {

                throw new IllegalArgumentException(
                        "Excel header row is missing."
                );
            }

            String[] requiredHeaders = {
                    "className",
                    "name",
                    "capacity",
                    "description"
            };

            for (int i = 0;
                 i < requiredHeaders.length;
                 i++) {

                String actualHeader =
                        formatter
                                .formatCellValue(
                                        headerRow.getCell(i)
                                )
                                .trim();

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

                Row row =
                        sheet.getRow(rowIndex);

                if (row == null) {
                    continue;
                }

                // ---------------------------------------------
                // EMPTY ROW CHECK
                // ---------------------------------------------

                boolean emptyRow = true;

                for (int i = 0; i < 4; i++) {

                    String value =
                            formatter
                                    .formatCellValue(
                                            row.getCell(i)
                                    )
                                    .trim();

                    if (!value.isEmpty()) {
                        emptyRow = false;
                        break;
                    }
                }

                if (emptyRow) {
                    continue;
                }

                totalRows++;

                int excelRowNumber =
                        rowIndex + 1;

                try {

                    // -----------------------------------------
                    // READ VALUES
                    // -----------------------------------------

                    String className =
                            formatter
                                    .formatCellValue(
                                            row.getCell(0)
                                    )
                                    .trim();

                    String name =
                            formatter
                                    .formatCellValue(
                                            row.getCell(1)
                                    )
                                    .trim();

                    String capacityValue =
                            formatter
                                    .formatCellValue(
                                            row.getCell(2)
                                    )
                                    .trim();

                    String description =
                            formatter
                                    .formatCellValue(
                                            row.getCell(3)
                                    )
                                    .trim();

                    // -----------------------------------------
                    // REQUIRED VALIDATION
                    // -----------------------------------------

                    if (className.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Class name is required."
                        );
                    }

                    if (name.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Section name is required."
                        );
                    }

                    if (capacityValue.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Capacity is required."
                        );
                    }

                    Integer capacity;

                    try {
                        double numericCapacity = Double.parseDouble(capacityValue);

                        if (!Double.isFinite(numericCapacity)
                                || numericCapacity < 0
                                || numericCapacity != Math.floor(numericCapacity)
                                || numericCapacity > Integer.MAX_VALUE) {
                            throw new IllegalArgumentException(
                                    "Capacity must be a non-negative whole number."
                            );
                        }

                        capacity = (int) numericCapacity;

                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Capacity must be a valid whole number."
                        );
                    }

                    if (capacity < 0) {

                        throw new IllegalArgumentException(
                                "Capacity cannot be negative."
                        );
                    }

                    // -----------------------------------------
                    // FIND CLASS BY NAME + SCHOOL
                    // -----------------------------------------

                    Optional<ClassEntity>
                            classOptional =
                            classRepository
                                    .findByNameIgnoreCaseAndSchoolId(
                                            className,
                                            schoolId
                                    );

                    if (classOptional.isEmpty()) {

                        throw new ResourceNotFoundException(
                                "Class not found with name '"
                                        + className
                                        + "' for school ID "
                                        + schoolId
                        );
                    }

                    ClassEntity classEntity =
                            classOptional.get();

                    // -----------------------------------------
                    // DUPLICATE CHECK INSIDE EXCEL
                    // -----------------------------------------

                    String duplicateKey =
                            classEntity.getId()
                                    + "|"
                                    + name.toLowerCase(
                                    Locale.ROOT
                            );

                    if (!processedSections
                            .add(duplicateKey)) {

                        throw new IllegalArgumentException(
                                "Duplicate section '"
                                        + name
                                        + "' for class '"
                                        + className
                                        + "' in Excel."
                        );
                    }

                    // -----------------------------------------
                    // FIND EXISTING SECTION
                    // -----------------------------------------

                    Optional<Section>
                            existingSectionOptional =
                            sectionRepository
                                    .findByNameIgnoreCaseAndClassEntityId(
                                            name,
                                            classEntity.getId()
                                    );

                    if (existingSectionOptional
                            .isPresent()) {

                        // -------------------------------------
                        // UPDATE
                        // -------------------------------------

                        Section existingSection =
                                existingSectionOptional
                                        .get();

                        existingSection
                                .setCapacity(capacity);

                        existingSection
                                .setDescription(
                                        description.isEmpty()
                                                ? null
                                                : description
                                );

                        sectionRepository.save(
                                existingSection
                        );

                        updatedCount++;

                    } else {

                        // -------------------------------------
                        // CREATE
                        // -------------------------------------

                        Section newSection =
                                Section.builder()
                                        .classEntity(
                                                classEntity
                                        )
                                        .name(name)
                                        .capacity(capacity)
                                        .description(
                                                description.isEmpty()
                                                        ? null
                                                        : description
                                        )
                                        .build();

                        sectionRepository.save(
                                newSection
                        );

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
                            "Failed to import section at row {}: {}",
                            excelRowNumber,
                            e.getMessage()
                    );
                }
            }
        }

        logger.info(
                "Section Excel import completed. Total: {}, Created: {}, Updated: {}, Failed: {}",
                totalRows,
                createdCount,
                updatedCount,
                failedCount
        );

        return SectionImportResponseDto.builder()
                .totalRows(totalRows)
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .failedCount(failedCount)
                .errors(errors)
                .message(
                        failedCount == 0
                                ? "Sections Excel imported successfully."
                                : "Sections Excel imported with some errors."
                )
                .build();
    }

    // =========================================================
    // RESPONSE MAPPER
    // =========================================================

    private SectionResponseDto mapToResponseDto(
            Section section) {

        return SectionResponseDto.builder()
                .id(section.getId())
                .classId(
                        section.getClassEntity() != null
                                ? section.getClassEntity().getId()
                                : null
                )
                .className(
                        section.getClassEntity() != null
                                ? section.getClassEntity().getName()
                                : null
                )
                .name(section.getName())
                .capacity(section.getCapacity())
                .description(section.getDescription())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .build();
    }
}