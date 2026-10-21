package com.harish.student_management.service;

import com.harish.student_management.dto.SubjectImportResponseDto;
import com.harish.student_management.dto.SubjectRequest;
import com.harish.student_management.dto.SubjectResponse;
import com.harish.student_management.entity.ClassSubject;
import com.harish.student_management.entity.School;
import com.harish.student_management.entity.Subject;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.ClassSubjectRepository;
import com.harish.student_management.repository.SchoolRepository;
import com.harish.student_management.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final SchoolRepository schoolRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final CurrentSchoolService currentSchoolService;

    private Long currentSchoolId() {
        return currentSchoolService.getCurrentSchoolId();
    }

    private void verifyRequestedSchool(Long requestedSchoolId) {
        if (requestedSchoolId == null
                || !currentSchoolId().equals(requestedSchoolId)) {
            throw new AccessDeniedException(
                    "You cannot access another school's subjects");
        }
    }

    private Subject getSubjectForCurrentSchool(Long id) {
        return subjectRepository.findByIdAndSchoolId(id, currentSchoolId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Subject not found: " + id));
    }

    @Override
    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        Long schoolId = currentSchoolId();

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Current school not found"));

        String code = normalizeCode(request.getCode());

        if (subjectRepository.findBySchoolIdAndCode(schoolId, code)
                .isPresent()) {
            throw new IllegalArgumentException(
                    "Subject code already exists for this school");
        }

        Subject subject = Subject.builder()
                .school(school)
                .name(request.getName().trim())
                .code(code)
                .description(request.getDescription())
                .status(normalizeStatus(request.getStatus()))
                .build();

        return mapToResponse(subjectRepository.save(subject));
    }

    @Override
    public List<SubjectResponse> getAllSubjects() {
        return subjectRepository.findBySchoolId(currentSchoolId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<SubjectResponse> getSubjectsBySchool(Long schoolId) {
        verifyRequestedSchool(schoolId);

        return subjectRepository.findBySchoolId(currentSchoolId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<SubjectResponse> getActiveSubjectsBySchool(Long schoolId) {
        verifyRequestedSchool(schoolId);

        return subjectRepository
                .findBySchoolIdAndStatus(currentSchoolId(), "ACTIVE")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public SubjectResponse getSubjectById(Long id) {
        return mapToResponse(getSubjectForCurrentSchool(id));
    }

    @Override
    @Transactional
    public SubjectResponse updateSubject(
            Long id, SubjectRequest request) {

        Subject subject = getSubjectForCurrentSchool(id);

        if (request.getCode() != null
                && !request.getCode().isBlank()) {

            String code = normalizeCode(request.getCode());

            boolean codeUsed = subjectRepository
                    .findBySchoolIdAndCode(
                            currentSchoolId(), code)
                    .filter(existing ->
                            !existing.getId().equals(subject.getId()))
                    .isPresent();

            if (codeUsed) {
                throw new IllegalArgumentException(
                        "Subject code already exists for this school");
            }

            subject.setCode(code);
        }

        if (request.getName() != null
                && !request.getName().isBlank()) {
            subject.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            subject.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {
            subject.setStatus(normalizeStatus(request.getStatus()));
        }

        // Do not change subject.school using request data.
        return mapToResponse(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public void deleteSubject(Long id) {
        Subject subject = getSubjectForCurrentSchool(id);
        subjectRepository.delete(subject);
    }

    @Override
    public List<SubjectResponse> getSubjectsByClass(Long classId) {
        Long schoolId = currentSchoolId();

        List<ClassSubject> assignments =
                classSubjectRepository.findByClassIdAndSchoolId(
                        classId, schoolId);

        return assignments.stream()
                .map(ClassSubject::getSubject)
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public SubjectImportResponseDto importSubjects(
            MultipartFile file, Long schoolId) throws IOException {

        verifyRequestedSchool(schoolId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null
                || !fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException(
                    "Only .xlsx Excel files are supported");
        }

        Long authenticatedSchoolId = currentSchoolId();

        School school = schoolRepository.findById(authenticatedSchoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Current school not found"));

        int totalRows = 0;
        int createdCount = 0;
        int updatedCount = 0;
        int failedCount = 0;

        List<String> errors = new ArrayList<>();
        Set<String> processedCodes = new HashSet<>();

        try (Workbook workbook =
                     WorkbookFactory.create(file.getInputStream())) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException(
                        "Excel workbook has no sheets");
            }

            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);

            if (header == null) {
                throw new IllegalArgumentException(
                        "Excel header row is missing");
            }

            String[] expectedHeaders = {
                    "code", "name", "description", "status"
            };

            DataFormatter formatter = new DataFormatter();

            for (int i = 0; i < expectedHeaders.length; i++) {
                String actual = getCellValue(
                        formatter, header.getCell(i));

                if (!expectedHeaders[i].equalsIgnoreCase(actual.trim())) {
                    throw new IllegalArgumentException(
                            "Invalid Excel headers. Expected order: "
                                    + String.join(", ", expectedHeaders));
                }
            }

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (isRowEmpty(formatter, row)) {
                    continue;
                }

                totalRows++;

                try {
                    String code = normalizeCode(
                            getCellValue(formatter, row.getCell(0)));

                    String name = getCellValue(
                            formatter, row.getCell(1)).trim();

                    String description = getCellValue(
                            formatter, row.getCell(2)).trim();

                    String status = normalizeStatus(
                            getCellValue(formatter, row.getCell(3)));

                    if (code.isBlank()) {
                        throw new IllegalArgumentException(
                                "Subject code is required");
                    }

                    if (name.isBlank()) {
                        throw new IllegalArgumentException(
                                "Subject name is required");
                    }

                    if (!processedCodes.add(code.toLowerCase(Locale.ROOT))) {
                        throw new IllegalArgumentException(
                                "Duplicate subject code in Excel: " + code);
                    }

                    var existing = subjectRepository.findBySchoolIdAndCode(
                            authenticatedSchoolId, code);

                    if (existing.isPresent()) {
                        Subject subject = existing.get();
                        subject.setName(name);
                        subject.setDescription(
                                description.isBlank() ? null : description);
                        subject.setStatus(status);

                        subjectRepository.save(subject);
                        updatedCount++;
                    } else {
                        Subject subject = Subject.builder()
                                .school(school)
                                .code(code)
                                .name(name)
                                .description(
                                        description.isBlank()
                                                ? null : description)
                                .status(status)
                                .build();

                        subjectRepository.save(subject);
                        createdCount++;
                    }

                } catch (IllegalArgumentException e) {
                    failedCount++;
                    errors.add("Row " + (rowIndex + 1) + ": "
                            + safeMessage(e));
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Unable to read the Excel file", e);
        }

        String message = failedCount == 0
                ? "Subjects imported successfully."
                : "Subjects import completed with some errors.";

        return SubjectImportResponseDto.builder()
                .totalRows(totalRows)
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .failedCount(failedCount)
                .errors(errors)
                .message(message)
                .build();
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);

        if (!normalized.equals("ACTIVE")
                && !normalized.equals("INACTIVE")) {
            throw new IllegalArgumentException(
                    "Status must be ACTIVE or INACTIVE");
        }

        return normalized;
    }

    private String getCellValue(DataFormatter formatter, Cell cell) {
        return cell == null ? "" : formatter.formatCellValue(cell);
    }

    private boolean isRowEmpty(DataFormatter formatter, Row row) {
        if (row == null) {
            return true;
        }

        for (int i = 0; i < 4; i++) {
            if (!getCellValue(formatter, row.getCell(i)).trim().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank()
                ? "Invalid row data" : message;
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
}