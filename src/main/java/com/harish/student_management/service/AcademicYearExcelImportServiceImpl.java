package com.harish.student_management.service;

import com.harish.student_management.dto.AcademicYearImportErrorDto;
import com.harish.student_management.dto.AcademicYearImportResponseDto;
import com.harish.student_management.entity.AcademicYear;
import com.harish.student_management.entity.School;
import com.harish.student_management.repository.AcademicYearRepository;
import com.harish.student_management.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AcademicYearExcelImportServiceImpl
        implements AcademicYearExcelImportService {

    private final AcademicYearRepository academicYearRepository;
    private final CurrentSchoolService currentSchoolService;
    private final SchoolRepository schoolRepository;

    @Override
    public AcademicYearImportResponseDto importAcademicYearsExcel(
            MultipartFile file) {

        int totalRows = 0;
        int created = 0;
        int updated = 0;
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() ->
                        new IllegalStateException("School not found"));

        List<AcademicYearImportErrorDto> errors = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() <= 1) {
                throw new IllegalArgumentException(
                        "Excel file does not contain any data rows"
                );
            }

            Row headerRow = sheet.getRow(0);

            validateHeaders(headerRow);

            boolean hasDataRows = false;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                hasDataRows = true;

                int excelRowNumber = i + 1;
                totalRows++;

                try {

                    String name = getStringValue(row.getCell(0));
                    LocalDate startDate = getDateValue(row.getCell(1));
                    LocalDate endDate = getDateValue(row.getCell(2));
                    Boolean current = getBooleanValue(row.getCell(3));
                    String description = getStringValue(row.getCell(4));

                    // ==============================
                    // VALIDATION
                    // ==============================

                    if (name == null || name.isBlank()) {
                        throw new IllegalArgumentException(
                                "Name is required"
                        );
                    }

                    if (startDate == null) {
                        throw new IllegalArgumentException(
                                "Start Date is required"
                        );
                    }

                    if (endDate == null) {
                        throw new IllegalArgumentException(
                                "End Date is required"
                        );
                    }

                    if (endDate.isBefore(startDate)) {
                        throw new IllegalArgumentException(
                                "End Date cannot be before Start Date"
                        );
                    }

                    if (current == null) {
                        current = false;
                    }

// FIND EXISTING — CURRENT SCHOOL ONLY

                    AcademicYear academicYear = academicYearRepository
                            .findBySchool_IdAndNameIgnoreCase(
                                    schoolId, name.trim())
                            .orElse(null);

                    boolean isNew = academicYear == null;

                    if (isNew) {
                        academicYear = AcademicYear.builder()
                                .school(school)
                                .name(name.trim())
                                .startDate(startDate)
                                .endDate(endDate)
                                .current(false)
                                .description(description)
                                .build();
                    } else {
                        academicYear.setName(name.trim());
                        academicYear.setStartDate(startDate);
                        academicYear.setEndDate(endDate);
                        academicYear.setDescription(description);
                    }

// CURRENT ACADEMIC YEAR — CURRENT SCHOOL ONLY


                    if (Boolean.TRUE.equals(current)) {

                        Long academicYearId = academicYear.getId();

                        academicYearRepository
                                .findBySchool_IdAndCurrentTrue(schoolId)
                                .filter(existing ->
                                        academicYearId == null
                                                || !existing.getId().equals(academicYearId))
                                .ifPresent(existing -> {
                                    existing.setCurrent(false);
                                    academicYearRepository.save(existing);
                                });
                    }

                    academicYear.setCurrent(Boolean.TRUE.equals(current));

                    academicYearRepository.save(academicYear);


                    academicYear.setCurrent(Boolean.TRUE.equals(current));

                    academicYearRepository.save(academicYear);

                    if (isNew) {
                        created++;
                    } else {
                        updated++;
                    }

                } catch (Exception e) {

                    errors.add(
                            AcademicYearImportErrorDto.builder()
                                    .row(excelRowNumber)
                                    .message(e.getMessage())
                                    .build()
                    );
                }
            }

            if (!hasDataRows) {
                throw new IllegalArgumentException(
                        "Excel file does not contain any data rows"
                );
            }
        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Failed to import Academic Years Excel: "
                            + e.getMessage(),
                    e
            );
        }

        return AcademicYearImportResponseDto.builder()
                .totalRows(totalRows)
                .created(created)
                .updated(updated)
                .errors(errors.size())
                .errorDetails(errors)
                .build();
    }

    // ============================================================
    // HEADER VALIDATION
    // ============================================================

    private void validateHeaders(Row headerRow) {

        if (headerRow == null) {
            throw new IllegalArgumentException(
                    "Excel header row is missing"
            );
        }

        String[] expectedHeaders = {
                "Name",
                "Start Date",
                "End Date",
                "Current",
                "Description"
        };

        for (int i = 0; i < expectedHeaders.length; i++) {

            String actual =
                    getStringValue(headerRow.getCell(i));

            if (actual == null
                    || !actual.trim()
                    .equalsIgnoreCase(expectedHeaders[i])) {

                throw new IllegalArgumentException(
                        "Invalid header at column "
                                + (i + 1)
                                + ". Expected: "
                                + expectedHeaders[i]
                );
            }
        }
    }

    // ============================================================
    // STRING
    // ============================================================

    private String getStringValue(Cell cell) {

        if (cell == null) {
            return null;
        }

        DataFormatter formatter = new DataFormatter();

        String value = formatter.formatCellValue(cell);

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    // ============================================================
    // DATE
    // ============================================================

    private LocalDate getDateValue(Cell cell) {

        if (cell == null) {
            return null;
        }

        try {

            if (cell.getCellType() == CellType.NUMERIC
                    && DateUtil.isCellDateFormatted(cell)) {

                return cell.getDateCellValue()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            }

            String value = getStringValue(cell);

            if (value == null) {
                return null;
            }

            return LocalDate.parse(value);

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Invalid date format. Use yyyy-MM-dd"
            );
        }
    }

    // ============================================================
    // BOOLEAN
    // ============================================================

    private Boolean getBooleanValue(Cell cell) {

        if (cell == null) {
            return false;
        }

        if (cell.getCellType() == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        }

        String value = getStringValue(cell);

        if (value == null) {
            return false;
        }

        if (value.equalsIgnoreCase("TRUE")
                || value.equalsIgnoreCase("YES")
                || value.equalsIgnoreCase("1")) {

            return true;
        }

        if (value.equalsIgnoreCase("FALSE")
                || value.equalsIgnoreCase("NO")
                || value.equalsIgnoreCase("0")) {

            return false;
        }

        throw new IllegalArgumentException(
                "Current must be TRUE or FALSE"
        );
    }

    // ============================================================
    // EMPTY ROW
    // ============================================================
    private boolean isRowEmpty(Row row) {

        if (row == null) {
            return true;
        }

        for (int i = 0; i < 5; i++) {

            Cell cell = row.getCell(
                    i,
                    Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
            );

            if (cell == null) {
                continue;
            }

            if (cell.getCellType() == CellType.BLANK) {
                continue;
            }

            if (cell.getCellType() == CellType.STRING
                    && cell.getStringCellValue().trim().isEmpty()) {
                continue;
            }

            return false;
        }

        return true;
    }
}