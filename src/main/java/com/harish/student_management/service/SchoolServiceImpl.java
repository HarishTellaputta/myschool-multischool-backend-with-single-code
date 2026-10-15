package com.harish.student_management.service;

import com.harish.student_management.dto.SchoolImportResponseDto;
import com.harish.student_management.dto.SchoolRequestDto;
import com.harish.student_management.dto.SchoolResponseDto;
import com.harish.student_management.entity.School;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SchoolServiceImpl implements SchoolService {

    private static final Logger logger =
            LoggerFactory.getLogger(SchoolServiceImpl.class);

    private final SchoolRepository schoolRepository;

    private static final List<String> REQUIRED_HEADERS = List.of(
            "name",
            "code",
            "address",
            "affiliationNumber",
            "city",
            "country",
            "email",
            "establishmentYear",
            "facilities",
            "facultyInfo",
            "logoUrl",
            "phone",
            "postalCode",
            "principalMessage",
            "principalName",
            "schoolType",
            "state",
            "status",
            "website"
    );

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    public SchoolResponseDto createSchool(SchoolRequestDto request) {

        logger.info(
                "Creating school with code: {}",
                request.getCode()
        );

        if (schoolRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException(
                    "School already exists with code: "
                            + request.getCode()
            );
        }

        School school = School.builder()
                .name(request.getName())
                .code(request.getCode())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .website(request.getWebsite())
                .logoUrl(request.getLogoUrl())
                .principalMessage(request.getPrincipalMessage())
                .principalName(request.getPrincipalName())
                .facilities(request.getFacilities())
                .facultyInfo(request.getFacultyInfo())
                .establishmentYear(request.getEstablishmentYear())
                .affiliationNumber(request.getAffiliationNumber())
                .schoolType(request.getSchoolType())
                .status(
                        request.getStatus() != null
                                ? request.getStatus()
                                : "ACTIVE"
                )
                .build();

        School savedSchool =
                schoolRepository.save(school);

        logger.info(
                "School created successfully with ID: {}",
                savedSchool.getId()
        );

        return mapToResponseDto(savedSchool);
    }

    // =========================================================
    // GET
    // =========================================================

    @Override
    public SchoolResponseDto getSchoolById(Long id) {

        School school =
                schoolRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "School not found with id: " + id
                                )
                        );

        return mapToResponseDto(school);
    }

    @Override
    public List<SchoolResponseDto> getAllSchools() {

        return schoolRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    public SchoolResponseDto updateSchool(
            Long id,
            SchoolRequestDto request
    ) {

        School school =
                schoolRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "School not found with id: " + id
                                )
                        );

        if (!school.getCode().equals(request.getCode())
                && schoolRepository.existsByCode(request.getCode())) {

            throw new IllegalArgumentException(
                    "School already exists with code: "
                            + request.getCode()
            );
        }

        school.setName(request.getName());
        school.setCode(request.getCode());
        school.setPhone(request.getPhone());
        school.setEmail(request.getEmail());
        school.setAddress(request.getAddress());
        school.setCity(request.getCity());
        school.setState(request.getState());
        school.setPostalCode(request.getPostalCode());
        school.setCountry(request.getCountry());
        school.setWebsite(request.getWebsite());
        school.setLogoUrl(request.getLogoUrl());
        school.setPrincipalMessage(request.getPrincipalMessage());
        school.setPrincipalName(request.getPrincipalName());
        school.setFacilities(request.getFacilities());
        school.setFacultyInfo(request.getFacultyInfo());
        school.setEstablishmentYear(
                request.getEstablishmentYear()
        );
        school.setAffiliationNumber(
                request.getAffiliationNumber()
        );
        school.setSchoolType(request.getSchoolType());
        school.setStatus(request.getStatus());

        School updatedSchool =
                schoolRepository.save(school);

        return mapToResponseDto(updatedSchool);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Override
    public void deleteSchool(Long id) {

        School school =
                schoolRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "School not found with id: " + id
                                )
                        );

        schoolRepository.delete(school);
    }

    // =========================================================
    // LOGO
    // =========================================================

    @Override
    public SchoolResponseDto uploadSchoolLogo(
            Long id,
            MultipartFile file
    ) throws IOException {

        School school =
                schoolRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "School not found with id: " + id
                                )
                        );

        if (file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select a file to upload"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !contentType.startsWith("image/")) {

            throw new IllegalArgumentException(
                    "Only image files are allowed"
            );
        }

        long maxSize = 5 * 1024 * 1024;

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    "File size should not exceed 5 MB"
            );
        }

        Path uploadPath =
                Paths.get("uploads/school-logos");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName =
                UUID.randomUUID()
                        + "_"
                        + file.getOriginalFilename();

        Path filePath =
                uploadPath.resolve(fileName);

        Files.copy(
                file.getInputStream(),
                filePath,
                StandardCopyOption.REPLACE_EXISTING
        );

        school.setLogoUrl(
                "/uploads/school-logos/" + fileName
        );

        School updatedSchool =
                schoolRepository.save(school);

        return mapToResponseDto(updatedSchool);
    }

    // =========================================================
    // EXCEL IMPORT
    // =========================================================

    @Override
    public SchoolImportResponseDto importSchools(
            MultipartFile file
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select an Excel file."
            );
        }

        String fileName =
                file.getOriginalFilename();

        if (fileName == null
                || !fileName.toLowerCase().endsWith(".xlsx")) {

            throw new IllegalArgumentException(
                    "Only .xlsx Excel files are supported."
            );
        }

        List<String> errors = new ArrayList<>();

        int totalRows = 0;
        int createdCount = 0;
        int updatedCount = 0;
        int failedCount = 0;

        Set<String> codesInCurrentFile =
                new HashSet<>();

        try (
                InputStream inputStream =
                        file.getInputStream();

                Workbook workbook =
                        new XSSFWorkbook(inputStream)
        ) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null) {
                throw new IllegalArgumentException(
                        "Excel sheet not found."
                );
            }

            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalArgumentException(
                        "Excel header row is missing."
                );
            }

            validateHeaders(headerRow);

            DataFormatter formatter =
                    new DataFormatter();

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (isEmptyRow(row, formatter)) {
                    continue;
                }

                totalRows++;

                int excelRowNumber =
                        rowIndex + 1;

                try {

                    String name =
                            getCellValue(
                                    row,
                                    0,
                                    formatter
                            );

                    String code =
                            getCellValue(
                                    row,
                                    1,
                                    formatter
                            );

                    if (name.isBlank()) {
                        throw new IllegalArgumentException(
                                "name is required"
                        );
                    }

                    if (code.isBlank()) {
                        throw new IllegalArgumentException(
                                "code is required"
                        );
                    }

                    if (!codesInCurrentFile.add(
                            code.toUpperCase()
                    )) {

                        throw new IllegalArgumentException(
                                "Duplicate code in Excel: "
                                        + code
                        );
                    }

                    Optional<School> existingSchool =
                            schoolRepository.findByCode(code);

                    School school;

                    if (existingSchool.isPresent()) {

                        school =
                                existingSchool.get();

                        updateSchoolFromExcel(
                                school,
                                row,
                                formatter
                        );

                        schoolRepository.save(school);

                        updatedCount++;

                    } else {

                        school =
                                buildSchoolFromExcel(
                                        row,
                                        formatter
                                );

                        schoolRepository.save(school);

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
                }
            }
        }

        String message;

        if (failedCount == 0) {
            message =
                    "School Excel imported successfully.";
        } else {
            message =
                    "School Excel import completed with errors.";
        }

        return SchoolImportResponseDto.builder()
                .totalRows(totalRows)
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .failedCount(failedCount)
                .errors(errors)
                .message(message)
                .build();
    }

    // =========================================================
    // BUILD NEW SCHOOL
    // =========================================================

    private School buildSchoolFromExcel(
            Row row,
            DataFormatter formatter
    ) {

        return School.builder()
                .name(getCellValue(row, 0, formatter))
                .code(getCellValue(row, 1, formatter))
                .address(getCellValue(row, 2, formatter))
                .affiliationNumber(
                        getCellValue(row, 3, formatter)
                )
                .city(getCellValue(row, 4, formatter))
                .country(getCellValue(row, 5, formatter))
                .email(getCellValue(row, 6, formatter))
                .establishmentYear(
                        getCellValue(row, 7, formatter)
                )
                .facilities(
                        getCellValue(row, 8, formatter)
                )
                .facultyInfo(
                        getCellValue(row, 9, formatter)
                )
                .logoUrl(
                        getCellValue(row, 10, formatter)
                )
                .phone(
                        getCellValue(row, 11, formatter)
                )
                .postalCode(
                        getCellValue(row, 12, formatter)
                )
                .principalMessage(
                        getCellValue(row, 13, formatter)
                )
                .principalName(
                        getCellValue(row, 14, formatter)
                )
                .schoolType(
                        getCellValue(row, 15, formatter)
                )
                .state(
                        getCellValue(row, 16, formatter)
                )
                .status(
                        getCellValue(row, 17, formatter)
                                .isBlank()
                                ? "ACTIVE"
                                : getCellValue(
                                row,
                                17,
                                formatter
                        )
                )
                .website(
                        getCellValue(row, 18, formatter)
                )
                .build();
    }

    // =========================================================
    // UPDATE EXISTING SCHOOL
    // =========================================================

    private void updateSchoolFromExcel(
            School school,
            Row row,
            DataFormatter formatter
    ) {

        String name =
                getCellValue(row, 0, formatter);

        if (!name.isBlank()) {
            school.setName(name);
        }

        school.setAddress(
                getCellValue(row, 2, formatter)
        );

        school.setAffiliationNumber(
                getCellValue(row, 3, formatter)
        );

        school.setCity(
                getCellValue(row, 4, formatter)
        );

        school.setCountry(
                getCellValue(row, 5, formatter)
        );

        school.setEmail(
                getCellValue(row, 6, formatter)
        );

        school.setEstablishmentYear(
                getCellValue(row, 7, formatter)
        );

        school.setFacilities(
                getCellValue(row, 8, formatter)
        );

        school.setFacultyInfo(
                getCellValue(row, 9, formatter)
        );

        school.setLogoUrl(
                getCellValue(row, 10, formatter)
        );

        school.setPhone(
                getCellValue(row, 11, formatter)
        );

        school.setPostalCode(
                getCellValue(row, 12, formatter)
        );

        school.setPrincipalMessage(
                getCellValue(row, 13, formatter)
        );

        school.setPrincipalName(
                getCellValue(row, 14, formatter)
        );

        school.setSchoolType(
                getCellValue(row, 15, formatter)
        );

        school.setState(
                getCellValue(row, 16, formatter)
        );

        String status =
                getCellValue(row, 17, formatter);

        school.setStatus(
                status.isBlank()
                        ? "ACTIVE"
                        : status
        );

        school.setWebsite(
                getCellValue(row, 18, formatter)
        );
    }

    // =========================================================
    // HEADER VALIDATION
    // =========================================================

    private void validateHeaders(Row headerRow) {

        DataFormatter formatter =
                new DataFormatter();

        List<String> actualHeaders =
                new ArrayList<>();

        for (int i = 0;
             i < REQUIRED_HEADERS.size();
             i++) {

            String value =
                    getCellValue(
                            headerRow,
                            i,
                            formatter
                    );

            actualHeaders.add(value);
        }

        for (String requiredHeader : REQUIRED_HEADERS) {

            if (!actualHeaders.contains(requiredHeader)) {

                throw new IllegalArgumentException(
                        "Missing required column: "
                                + requiredHeader
                );
            }
        }
    }

    // =========================================================
    // CELL VALUE
    // =========================================================

    private String getCellValue(
            Row row,
            int columnIndex,
            DataFormatter formatter
    ) {

        Cell cell =
                row.getCell(
                        columnIndex,
                        Row.MissingCellPolicy
                                .RETURN_BLANK_AS_NULL
                );

        if (cell == null) {
            return "";
        }

        return formatter
                .formatCellValue(cell)
                .trim();
    }

    // =========================================================
    // EMPTY ROW
    // =========================================================

    private boolean isEmptyRow(
            Row row,
            DataFormatter formatter
    ) {

        if (row == null) {
            return true;
        }

        for (int i = 0;
             i < REQUIRED_HEADERS.size();
             i++) {

            if (!getCellValue(
                    row,
                    i,
                    formatter
            ).isBlank()) {

                return false;
            }
        }

        return true;
    }

    // =========================================================
    // RESPONSE MAPPER
    // =========================================================

    private SchoolResponseDto mapToResponseDto(
            School school
    ) {

        return SchoolResponseDto.builder()
                .id(school.getId())
                .name(school.getName())
                .code(school.getCode())
                .phone(school.getPhone())
                .email(school.getEmail())
                .address(school.getAddress())
                .city(school.getCity())
                .state(school.getState())
                .postalCode(school.getPostalCode())
                .country(school.getCountry())
                .website(school.getWebsite())
                .logoUrl(school.getLogoUrl())
                .principalMessage(
                        school.getPrincipalMessage()
                )
                .principalName(
                        school.getPrincipalName()
                )
                .facilities(
                        school.getFacilities()
                )
                .facultyInfo(
                        school.getFacultyInfo()
                )
                .establishmentYear(
                        school.getEstablishmentYear()
                )
                .affiliationNumber(
                        school.getAffiliationNumber()
                )
                .schoolType(
                        school.getSchoolType()
                )
                .status(school.getStatus())
                .createdAt(school.getCreatedAt())
                .updatedAt(school.getUpdatedAt())
                .build();
    }
}