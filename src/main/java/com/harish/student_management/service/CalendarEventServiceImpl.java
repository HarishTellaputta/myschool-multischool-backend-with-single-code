
        package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.*;
import com.harish.student_management.exception.ResourceNotFoundException;
import com.harish.student_management.repository.*;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CalendarEventServiceImpl implements CalendarEventService {

 private final CalendarEventRepository repo;
 private final ClassRepository classes;
 private final SectionRepository sections;

 // =========================================================
 // MANUAL CREATE
 // =========================================================

 @Override
 public CalendarEventResponseDto create(CalendarEventRequestDto r) {
  return map(repo.save(apply(new CalendarEvent(), r)));
 }

 // =========================================================
 // MANUAL UPDATE
 // =========================================================

 @Override
 public CalendarEventResponseDto update(Long id, CalendarEventRequestDto r) {
  return map(apply(find(id), r));
 }

 // =========================================================
 // MANUAL DELETE
 // =========================================================

 @Override
 public void delete(Long id) {
  repo.delete(find(id));
 }

 // =========================================================
 // LIST
 // =========================================================

 @Override
 @Transactional(readOnly = true)
 public List<CalendarEventResponseDto> list(
         LocalDate from,
         LocalDate to,
         String audience,
         Long classId) {

  LocalDate f = from == null ? LocalDate.now() : from;
  LocalDate t = to == null ? f : to;

  if (t.isBefore(f)) {
   throw new IllegalArgumentException(
           "End date must not precede start date"
   );
  }

  List<CalendarEvent> es =
          repo.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(t, f);

  return es.stream()
          .filter(e ->
                  audience == null
                          || "ALL".equalsIgnoreCase(e.getAudience())
                          || audience.equalsIgnoreCase(e.getAudience())
          )
          .filter(e ->
                  classId == null
                          || e.getClassEntity() == null
                          || e.getClassEntity().getId().equals(classId)
          )
          .filter(e ->
                  "PUBLISHED".equalsIgnoreCase(e.getStatus())
          )
          .map(this::map)
          .toList();
 }

 // =========================================================
 // EXCEL IMPORT
 //
 // ID empty  -> CREATE
 // ID exists -> UPDATE
 // =========================================================

 @Override
 public CalendarEventImportResponseDto importExcel(MultipartFile file) {

  if (file == null || file.isEmpty()) {
   throw new IllegalArgumentException("Excel file is required");
  }

  String fileName = file.getOriginalFilename();

  if (fileName == null
          || !fileName.toLowerCase().endsWith(".xlsx")) {

   throw new IllegalArgumentException(
           "Only .xlsx Excel files are supported"
   );
  }

  int totalRows = 0;
  int created = 0;
  int updated = 0;

  List<CalendarEventImportErrorDto> errors = new ArrayList<>();

  try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

   if (workbook.getNumberOfSheets() == 0) {
    throw new IllegalArgumentException(
            "Excel file does not contain any sheet"
    );
   }

   Sheet sheet = workbook.getSheetAt(0);

   if (sheet.getPhysicalNumberOfRows() <= 1) {
    throw new IllegalArgumentException(
            "Excel file does not contain any data rows"
    );
   }

   // -------------------------------------------------
   // Validate header
   // -------------------------------------------------

   Row header = sheet.getRow(0);

   if (header == null) {
    throw new IllegalArgumentException(
            "Excel header row is missing"
    );
   }

   validateHeader(header);

   // -------------------------------------------------
   // Process rows
   // -------------------------------------------------

   for (int i = 1; i <= sheet.getLastRowNum(); i++) {

    Row row = sheet.getRow(i);

    if (row == null || isRowEmpty(row)) {
     continue;
    }

    totalRows++;

    int excelRowNumber = i + 1;

    try {

     Long id = getLongValue(row.getCell(0));

     String title = getStringValue(row.getCell(1));
     String description = getStringValue(row.getCell(2));
     String eventType = getStringValue(row.getCell(3));

     LocalDate startDate =
             getDateValue(row.getCell(4));

     LocalDate endDate =
             getDateValue(row.getCell(5));

     String audience =
             getStringValue(row.getCell(6));

     Long classId =
             getLongValue(row.getCell(7));

     Long sectionId =
             getLongValue(row.getCell(8));

     String status =
             getStringValue(row.getCell(9));

     // -------------------------------------------------
     // Build DTO
     // -------------------------------------------------

     CalendarEventRequestDto request =
             new CalendarEventRequestDto();

     request.setTitle(title);
     request.setDescription(description);
     request.setEventType(eventType);
     request.setStartDate(startDate);
     request.setEndDate(endDate);
     request.setAudience(audience);
     request.setClassId(classId);
     request.setSectionId(sectionId);
     request.setStatus(status);

     // -------------------------------------------------
     // CREATE
     // -------------------------------------------------

     if (id == null) {

      CalendarEvent event =
              apply(new CalendarEvent(), request);

      repo.save(event);

      created++;

     }

     // -------------------------------------------------
     // UPDATE
     // -------------------------------------------------

     else {

      CalendarEvent event = find(id);

      apply(event, request);

      repo.save(event);

      updated++;
     }

    } catch (Exception ex) {

     errors.add(
             CalendarEventImportErrorDto.builder()
                     .row(excelRowNumber)
                     .message(getErrorMessage(ex))
                     .build()
     );
    }
   }

  } catch (IOException ex) {

   throw new IllegalArgumentException(
           "Unable to read Excel file: " + ex.getMessage()
   );
  }

  return CalendarEventImportResponseDto.builder()
          .totalRows(totalRows)
          .created(created)
          .updated(updated)
          .errors(errors.size())
          .errorDetails(errors)
          .build();
 }

 // =========================================================
 // HEADER VALIDATION
 // =========================================================

 private void validateHeader(Row header) {

  String[] expectedHeaders = {
          "ID",
          "Title",
          "Description",
          "Event Type",
          "Start Date",
          "End Date",
          "Audience",
          "Class ID",
          "Section ID",
          "Status"
  };

  for (int i = 0; i < expectedHeaders.length; i++) {

   String actual =
           getStringValue(header.getCell(i));

   if (!expectedHeaders[i].equalsIgnoreCase(actual)) {

    throw new IllegalArgumentException(
            "Invalid Excel header at column "
                    + (i + 1)
                    + ". Expected: "
                    + expectedHeaders[i]
    );
   }
  }
 }

 // =========================================================
 // COMMON VALIDATION + ENTITY MAPPING
 // =========================================================

 private CalendarEvent apply(
         CalendarEvent e,
         CalendarEventRequestDto r) {

  if (r.getStartDate() == null) {
   throw new IllegalArgumentException(
           "Start date is required"
   );
  }

  String type =
          r.getEventType() == null
                  ? ""
                  : r.getEventType().trim().toUpperCase();

  Set<String> types = Set.of(
          "HOLIDAY",
          "EXAM",
          "SCHOOL_EVENT",
          "PARENT_MEETING",
          "IMPORTANT_DATE"
  );

  if (!types.contains(type)) {
   throw new IllegalArgumentException(
           "Unsupported event type: " + type
   );
  }

  if (r.getEndDate() != null
          && r.getEndDate().isBefore(r.getStartDate())) {

   throw new IllegalArgumentException(
           "End date must not precede start date"
   );
  }

  String audience =
          r.getAudience() == null
                  ? ""
                  : r.getAudience().trim().toUpperCase();

  Set<String> audiences = Set.of(
          "ALL",
          "PARENTS",
          "TEACHERS",
          "STUDENTS"
  );

  if (!audiences.contains(audience)) {
   throw new IllegalArgumentException(
           "Unsupported audience: " + audience
   );
  }

  if (r.getSectionId() != null
          && r.getClassId() == null) {

   throw new IllegalArgumentException(
           "Class is required when a section is selected"
   );
  }

  e.setTitle(
          r.getTitle() == null
                  ? null
                  : r.getTitle().trim()
  );

  e.setDescription(r.getDescription());

  e.setEventType(type);

  e.setStartDate(r.getStartDate());

  e.setEndDate(
          r.getEndDate() == null
                  ? r.getStartDate()
                  : r.getEndDate()
  );

  e.setAudience(audience);

  e.setStatus(
          r.getStatus() == null
                  ? "PUBLISHED"
                  : r.getStatus().trim().toUpperCase()
  );

  e.setClassEntity(
          r.getClassId() == null
                  ? null
                  : classes.findById(r.getClassId())
                  .orElseThrow(() ->
                          new ResourceNotFoundException(
                                  "Class not found: "
                                          + r.getClassId()
                          )
                  )
  );

  e.setSection(
          r.getSectionId() == null
                  ? null
                  : sections.findById(r.getSectionId())
                  .orElseThrow(() ->
                          new ResourceNotFoundException(
                                  "Section not found: "
                                          + r.getSectionId()
                          )
                  )
  );

  if (e.getSection() != null
          && !e.getSection()
          .getClassEntity()
          .getId()
          .equals(e.getClassEntity().getId())) {

   throw new IllegalArgumentException(
           "Section does not belong to class"
   );
  }

  return e;
 }

 // =========================================================
 // FIND
 // =========================================================

 private CalendarEvent find(Long id) {

  return repo.findById(id)
          .orElseThrow(() ->
                  new ResourceNotFoundException(
                          "Calendar event not found: " + id
                  )
          );
 }

 // =========================================================
 // EXCEL STRING VALUE
 // =========================================================

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

 // =========================================================
 // EXCEL LONG VALUE
 // =========================================================

 private Long getLongValue(Cell cell) {

  if (cell == null
          || cell.getCellType() == CellType.BLANK) {

   return null;
  }

  try {

   if (cell.getCellType() == CellType.NUMERIC) {
    return (long) cell.getNumericCellValue();
   }

   String value = getStringValue(cell);

   if (value == null || value.isBlank()) {
    return null;
   }

   return Long.parseLong(value);

  } catch (Exception ex) {

   throw new IllegalArgumentException(
           "Invalid numeric value: "
                   + getStringValue(cell)
   );
  }
 }

 // =========================================================
 // EXCEL DATE VALUE
 // =========================================================

 private LocalDate getDateValue(Cell cell) {

  if (cell == null
          || cell.getCellType() == CellType.BLANK) {

   return null;
  }

  try {

   if (cell.getCellType() == CellType.NUMERIC
           && DateUtil.isCellDateFormatted(cell)) {

    return cell.getLocalDateTimeCellValue()
            .toLocalDate();
   }

   String value = getStringValue(cell);

   if (value == null || value.isBlank()) {
    return null;
   }

   List<DateTimeFormatter> formatters = List.of(
           DateTimeFormatter.ofPattern("dd-MM-yyyy"),
           DateTimeFormatter.ofPattern("dd/MM/yyyy"),
           DateTimeFormatter.ofPattern("yyyy-MM-dd"),
           DateTimeFormatter.ofPattern("MM/dd/yyyy")
   );

   for (DateTimeFormatter formatter : formatters) {

    try {
     return LocalDate.parse(value, formatter);
    } catch (DateTimeParseException ignored) {
    }
   }

   throw new IllegalArgumentException(
           "Invalid date: " + value
   );

  } catch (IllegalArgumentException ex) {

   throw ex;

  } catch (Exception ex) {

   throw new IllegalArgumentException(
           "Invalid date value"
   );
  }
 }

 // =========================================================
 // EMPTY ROW CHECK
 // =========================================================

 private boolean isRowEmpty(Row row) {

  for (int i = 0; i < 10; i++) {

   Cell cell = row.getCell(i);

   if (cell != null
           && cell.getCellType() != CellType.BLANK
           && getStringValue(cell) != null) {

    return false;
   }
  }

  return true;
 }

 // =========================================================
 // ERROR MESSAGE
 // =========================================================

 private String getErrorMessage(Exception ex) {

  if (ex.getMessage() != null
          && !ex.getMessage().isBlank()) {

   return ex.getMessage();
  }

  return ex.getClass().getSimpleName();
 }

 // =========================================================
 // RESPONSE MAPPING
 // =========================================================

 private CalendarEventResponseDto map(CalendarEvent e) {

  return CalendarEventResponseDto.builder()
          .id(e.getId())
          .title(e.getTitle())
          .description(e.getDescription())
          .eventType(e.getEventType())
          .audience(e.getAudience())
          .status(e.getStatus())
          .startDate(e.getStartDate())
          .endDate(e.getEndDate())
          .classId(
                  e.getClassEntity() == null
                          ? null
                          : e.getClassEntity().getId()
          )
          .className(
                  e.getClassEntity() == null
                          ? null
                          : e.getClassEntity().getName()
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
          .build();
 }
}

