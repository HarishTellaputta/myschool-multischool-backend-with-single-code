package com.harish.student_management.service;


import com.harish.student_management.dto.SubjectImportResponseDto;
import com.harish.student_management.dto.SubjectRequest;
import com.harish.student_management.dto.SubjectResponse;
import com.harish.student_management.entity.School;
import com.harish.student_management.entity.Subject;
import com.harish.student_management.repository.ClassSubjectRepository;
import com.harish.student_management.repository.SchoolRepository;
import com.harish.student_management.repository.SubjectRepository;
import com.harish.student_management.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.harish.student_management.dto.SubjectImportResponseDto;

import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final SchoolRepository schoolRepository;
    private final ClassSubjectRepository classSubjectRepository;

    @Override
    public SubjectResponse createSubject(SubjectRequest request) {

        if (request.getSchoolId() == null) {
            throw new RuntimeException("School ID is required");
        }

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() ->
                        new RuntimeException("School not found"));

        if (subjectRepository.existsBySchoolIdAndCode(
                request.getSchoolId(),
                request.getCode())) {

            throw new RuntimeException(
                    "Subject code already exists for this school"
            );
        }

        Subject subject = Subject.builder()
                .school(school)
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .status(
                        request.getStatus() == null
                                ? "ACTIVE"
                                : request.getStatus()
                )
                .build();

        return mapToResponse(subjectRepository.save(subject));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getAllSubjects() {

        return subjectRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjectsBySchool(Long schoolId) {

        return subjectRepository.findBySchoolId(schoolId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getActiveSubjectsBySchool(Long schoolId) {

        return subjectRepository
                .findBySchoolIdAndStatus(schoolId, "ACTIVE")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(Long id) {

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Subject not found"));

        return mapToResponse(subject);
    }

    @Override
    public SubjectResponse updateSubject(
            Long id,
            SubjectRequest request) {

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Subject not found"));

        if (request.getCode() != null &&
                !request.getCode().equals(subject.getCode())) {

            if (subjectRepository.existsBySchoolIdAndCode(
                    subject.getSchool().getId(),
                    request.getCode())) {

                throw new RuntimeException(
                        "Subject code already exists for this school"
                );
            }

            subject.setCode(request.getCode());
        }

        if (request.getName() != null) {
            subject.setName(request.getName());
        }

        if (request.getDescription() != null) {
            subject.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {
            subject.setStatus(request.getStatus());
        }

        return mapToResponse(subjectRepository.save(subject));
    }

    @Override
    public void deleteSubject(Long id) {

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Subject not found"));

        subjectRepository.delete(subject);
    }

    private SubjectResponse mapToResponse(Subject subject) {

        return SubjectResponse.builder()
                .id(subject.getId())
                .schoolId(subject.getSchool().getId())
                .name(subject.getName())
                .code(subject.getCode())
                .description(subject.getDescription())
                .status(subject.getStatus())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjectsByClass(Long classId) {

        return classSubjectRepository
                .findByClassEntityId(classId)
                .stream()
                .map(classSubject -> mapToResponse(classSubject.getSubject()))
                .toList();
    }

    @Override
    public SubjectImportResponseDto importSubjects(
            MultipartFile file,
            Long schoolId
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Excel file is required");
        }

        if (schoolId == null) {
            throw new RuntimeException("School ID is required");
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null ||
                !fileName.toLowerCase().endsWith(".xlsx")) {

            throw new RuntimeException(
                    "Only .xlsx Excel files are supported"
            );
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() ->
                        new RuntimeException("School not found"));

        int totalRows = 0;
        int createdCount = 0;
        int updatedCount = 0;
        int failedCount = 0;

        List<String> errors = new ArrayList<>();

        Set<String> processedCodes = new HashSet<>();

        try (Workbook workbook =
                     WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() <= 1) {
                throw new RuntimeException(
                        "Excel file does not contain any subject data"
                );
            }

            // ============================
            // HEADER VALIDATION
            // ============================

            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new RuntimeException(
                        "Excel header row is missing"
                );
            }

            String[] expectedHeaders = {
                    "code",
                    "name",
                    "description",
                    "status"
            };

            for (int i = 0; i < expectedHeaders.length; i++) {

                String actualHeader =
                        getCellValue(headerRow.getCell(i));

                if (!expectedHeaders[i]
                        .equalsIgnoreCase(actualHeader.trim())) {

                    throw new RuntimeException(
                            "Invalid Excel headers. Expected order: "
                                    + String.join(
                                    ", ",
                                    expectedHeaders
                            )
                    );
                }
            }

            // ============================
            // READ DATA ROWS
            // ============================

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                totalRows++;

                try {

                    String code =
                            getCellValue(row.getCell(0)).trim();

                    String name =
                            getCellValue(row.getCell(1)).trim();

                    String description =
                            getCellValue(row.getCell(2)).trim();

                    String status =
                            getCellValue(row.getCell(3)).trim();

                    // ============================
                    // REQUIRED VALIDATION
                    // ============================

                    if (code.isEmpty()) {
                        throw new RuntimeException(
                                "Subject code is required"
                        );
                    }

                    if (name.isEmpty()) {
                        throw new RuntimeException(
                                "Subject name is required"
                        );
                    }

                    if (status.isEmpty()) {
                        status = "ACTIVE";
                    }

                    status = status.toUpperCase();

                    // ============================
                    // DUPLICATE IN SAME EXCEL
                    // ============================

                    String duplicateKey =
                            code.toLowerCase();

                    if (!processedCodes.add(duplicateKey)) {

                        throw new RuntimeException(
                                "Duplicate subject code in Excel: "
                                        + code
                        );
                    }

                    // ============================
                    // FIND EXISTING SUBJECT
                    // ============================

                    var existingSubject =
                            subjectRepository
                                    .findBySchoolIdAndCode(
                                            schoolId,
                                            code
                                    );

                    if (existingSubject.isPresent()) {

                        Subject subject =
                                existingSubject.get();

                        subject.setName(name);
                        subject.setDescription(
                                description.isEmpty()
                                        ? null
                                        : description
                        );
                        subject.setStatus(status);

                        subjectRepository.save(subject);

                        updatedCount++;

                    } else {

                        Subject subject =
                                Subject.builder()
                                        .school(school)
                                        .code(code)
                                        .name(name)
                                        .description(
                                                description.isEmpty()
                                                        ? null
                                                        : description
                                        )
                                        .status(status)
                                        .build();

                        subjectRepository.save(subject);

                        createdCount++;
                    }

                } catch (Exception e) {

                    failedCount++;

                    errors.add(
                            "Row " +
                                    (rowIndex + 1) +
                                    ": " +
                                    e.getMessage()
                    );
                }
            }
        }

        String message;

        if (failedCount == 0) {
            message =
                    "Subjects imported successfully.";
        } else {
            message =
                    "Subjects import completed with some errors.";
        }

        return SubjectImportResponseDto.builder()
                .totalRows(totalRows)
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .failedCount(failedCount)
                .errors(errors)
                .message(message)
                .build();
    }


    private String getCellValue(Cell cell) {

        if (cell == null) {
            return "";
        }

        DataFormatter formatter = new DataFormatter();

        return formatter.formatCellValue(cell);
    }

    private boolean isRowEmpty(Row row) {

        if (row == null) {
            return true;
        }

        for (int i = 0; i < 4; i++) {

            Cell cell = row.getCell(i);

            if (cell != null &&
                    !getCellValue(cell).trim().isEmpty()) {

                return false;
            }
        }

        return true;
    }
}