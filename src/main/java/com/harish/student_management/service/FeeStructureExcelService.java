package com.harish.student_management.service;

import com.harish.student_management.dto.FeeStructureImportErrorDto;
import com.harish.student_management.dto.FeeStructureImportResultDto;
import com.harish.student_management.entity.AcademicYear;
import com.harish.student_management.entity.ClassEntity;
import com.harish.student_management.entity.FeeStructure;
import com.harish.student_management.repository.AcademicYearRepository;
import com.harish.student_management.repository.ClassRepository;
import com.harish.student_management.repository.FeeStructureRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FeeStructureExcelService {

    private final FeeStructureRepository feeStructureRepository;
    private final ClassRepository classRepository;
    private final AcademicYearRepository academicYearRepository;

    public FeeStructureImportResultDto importExcel(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }

        String filename = file.getOriginalFilename();

        if (filename == null ||
                !(filename.toLowerCase().endsWith(".xlsx")
                        || filename.toLowerCase().endsWith(".xls"))) {
            throw new IllegalArgumentException(
                    "Only Excel files (.xlsx or .xls) are allowed"
            );
        }

        FeeStructureImportResultDto result =
                FeeStructureImportResultDto.builder().build();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel file has no sheets");
            }

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() <= 1) {
                throw new IllegalArgumentException(
                        "Excel file contains no data rows"
                );
            }

            Row headerRow = sheet.getRow(0);

            validateHeaders(headerRow);

            int totalRows = 0;
            int created = 0;
            int skipped = 0;
            int errors = 0;

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (isEmptyRow(row)) {
                    continue;
                }

                totalRows++;

                int excelRowNumber = rowIndex + 1;

                try {

                    String feeName = getString(row.getCell(0));
                    String className = getString(row.getCell(1));
                    String academicYearName = getString(row.getCell(2));
                    BigDecimal amount = getAmount(row.getCell(3));
                    LocalDate dueDate = getDate(row.getCell(4));
                    String status = getString(row.getCell(5));

                    // Required fields
                    if (feeName.isBlank()) {
                        throw new IllegalArgumentException(
                                "Fee Name is required"
                        );
                    }

                    if (className.isBlank()) {
                        throw new IllegalArgumentException(
                                "Class is required"
                        );
                    }

                    if (academicYearName.isBlank()) {
                        throw new IllegalArgumentException(
                                "Academic Year is required"
                        );
                    }

                    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException(
                                "Amount must be greater than 0"
                        );
                    }

                    if (dueDate == null) {
                        throw new IllegalArgumentException(
                                "Due Date is required"
                        );
                    }

                    /*
                     * Find class by name.
                     *
                     * NOTE:
                     * This currently uses the repository's existing
                     * findByName method. If your ClassRepository does not
                     * have it, we will add the exact method there.
                     */
                    ClassEntity classEntity =
                            classRepository.findByNameIgnoreCase(className.trim())
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "Class not found: " + className
                                            ));

                    AcademicYear academicYear =
                            academicYearRepository.findByNameIgnoreCase(
                                            academicYearName.trim()
                                    )
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "Academic Year not found: "
                                                            + academicYearName
                                            ));

                    String normalizedFeeName = feeName.trim();

                    boolean duplicate =
                            feeStructureRepository
                                    .existsByNameAndClassEntityIdAndAcademicYearId(
                                            normalizedFeeName,
                                            classEntity.getId(),
                                            academicYear.getId()
                                    );

                    if (duplicate) {
                        skipped++;

                        result.getErrorDetails().add(
                                FeeStructureImportErrorDto.builder()
                                        .row(excelRowNumber)
                                        .message(
                                                "Fee structure already exists: "
                                                        + normalizedFeeName
                                                        + " for "
                                                        + className
                                                        + " / "
                                                        + academicYearName
                                        )
                                        .build()
                        );

                        continue;
                    }

                    if (status == null || status.isBlank()) {
                        status = "ACTIVE";
                    }

                    status = status.trim().toUpperCase();

                    if (!status.equals("ACTIVE")
                            && !status.equals("INACTIVE")) {

                        throw new IllegalArgumentException(
                                "Status must be ACTIVE or INACTIVE"
                        );
                    }

                    FeeStructure feeStructure =
                            FeeStructure.builder()
                                    .name(normalizedFeeName)
                                    .classEntity(classEntity)
                                    .academicYear(academicYear)
                                    .amount(amount.setScale(2))
                                    .dueDate(dueDate)
                                    .status(status)
                                    .build();

                    feeStructureRepository.save(feeStructure);

                    created++;

                } catch (Exception ex) {

                    errors++;

                    result.getErrorDetails().add(
                            FeeStructureImportErrorDto.builder()
                                    .row(excelRowNumber)
                                    .message(
                                            ex.getMessage() == null
                                                    ? "Invalid row"
                                                    : ex.getMessage()
                                    )
                                    .build()
                    );
                }
            }

            result.setTotalRows(totalRows);
            result.setCreated(created);
            result.setSkipped(skipped);
            result.setErrors(errors);

            return result;

        } catch (IOException e) {

            throw new IllegalArgumentException(
                    "Unable to read Excel file: " + e.getMessage()
            );
        }
    }

    private void validateHeaders(Row headerRow) {

        if (headerRow == null) {
            throw new IllegalArgumentException(
                    "Excel header row is missing"
            );
        }

        String[] expectedHeaders = {
                "Fee Name",
                "Class",
                "Academic Year",
                "Amount",
                "Due Date",
                "Status"
        };

        for (int i = 0; i < expectedHeaders.length; i++) {

            String actual = getString(headerRow.getCell(i));

            if (!expectedHeaders[i].equalsIgnoreCase(actual.trim())) {

                throw new IllegalArgumentException(
                        "Invalid column " + (i + 1)
                                + ". Expected: "
                                + expectedHeaders[i]
                );
            }
        }
    }

    private String getString(Cell cell) {

        if (cell == null) {
            return "";
        }

        DataFormatter formatter = new DataFormatter();

        return formatter.formatCellValue(cell).trim();
    }

    private BigDecimal getAmount(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }

        String value = getString(cell);

        if (value.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(
                    value.replace(",", "").trim()
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid amount: " + value
            );
        }
    }

    private LocalDate getDate(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {

            return cell.getDateCellValue()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }

        String value = getString(cell);

        if (value.isBlank()) {
            return null;
        }

        List<String> formats = Arrays.asList(
                "dd-MM-yyyy",
                "dd/MM/yyyy",
                "yyyy-MM-dd"
        );

        for (String format : formats) {

            try {
                return LocalDate.parse(
                        value,
                        java.time.format.DateTimeFormatter.ofPattern(format)
                );
            } catch (Exception ignored) {
            }
        }

        throw new IllegalArgumentException(
                "Invalid Due Date: " + value
                        + ". Use dd-MM-yyyy"
        );
    }

    private boolean isEmptyRow(Row row) {

        if (row == null) {
            return true;
        }

        for (int i = 0; i < 6; i++) {

            Cell cell = row.getCell(i);

            if (cell != null
                    && !getString(cell).isBlank()) {
                return false;
            }
        }

        return true;
    }


    public byte[] generateTemplate() {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Fee Structures");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            headerStyle.setFont(headerFont);

            // Headers
            String[] headers = {
                    "Fee Name",
                    "Class",
                    "Academic Year",
                    "Amount",
                    "Due Date",
                    "Status"
            };

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {

                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            /*
             * Get all classes from database.
             *
             * No class is hardcoded here.
             */
            List<ClassEntity> classes = classRepository.findAll();

            int rowIndex = 1;

            for (ClassEntity classEntity : classes) {

                Row row = sheet.createRow(rowIndex++);

                // Fee Name
                row.createCell(0)
                        .setCellValue("Tuition Fee");

                // Class
                row.createCell(1)
                        .setCellValue(classEntity.getName());

                // Academic Year
                row.createCell(2)
                        .setCellValue("2026-2027");

                // Amount
                row.createCell(3)
                        .setCellValue(10000);

                // Due Date
                row.createCell(4)
                        .setCellValue("10-06-2026");

                // Status
                row.createCell(5)
                        .setCellValue("ACTIVE");
            }

            // Column widths
            sheet.setColumnWidth(0, 25 * 256);
            sheet.setColumnWidth(1, 20 * 256);
            sheet.setColumnWidth(2, 20 * 256);
            sheet.setColumnWidth(3, 15 * 256);
            sheet.setColumnWidth(4, 18 * 256);
            sheet.setColumnWidth(5, 15 * 256);

            workbook.write(outputStream);

            return outputStream.toByteArray();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Unable to generate fee structure Excel template",
                    e
            );
        }
    }
}