package com.harish.student_management.controller;

import com.harish.student_management.dto.TeacherTimetableRequestDto;
import com.harish.student_management.dto.TeacherTimetableResponseDto;
import com.harish.student_management.dto.TimetableImportResponseDto;
import com.harish.student_management.service.TeacherTimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-timetables")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TeacherTimetableController {

    private final TeacherTimetableService timetableService;

    @Operation(summary = "Create timetable entry")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PostMapping
    public ResponseEntity<TeacherTimetableResponseDto> createTimetableEntry(@Valid @RequestBody TeacherTimetableRequestDto request) {
        TeacherTimetableResponseDto response = timetableService.createTimetableEntry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get timetable entry by ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<TeacherTimetableResponseDto> getTimetableById(@PathVariable Long id) {
        TeacherTimetableResponseDto response = timetableService.getTimetableById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get timetable by teacher ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<TeacherTimetableResponseDto>> getTimetableByTeacherId(@PathVariable Long teacherId) {
        List<TeacherTimetableResponseDto> response = timetableService.getTimetableByTeacherId(teacherId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get timetable by teacher ID and day of week")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/teacher/{teacherId}/day/{dayOfWeek}")
    public ResponseEntity<List<TeacherTimetableResponseDto>> getTimetableByTeacherIdAndDayOfWeek(
            @PathVariable Long teacherId, @PathVariable String dayOfWeek) {
        List<TeacherTimetableResponseDto> response = timetableService.getTimetableByTeacherIdAndDayOfWeek(teacherId, dayOfWeek);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get timetable by class ID")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    @GetMapping("/class/{classId}")
    public ResponseEntity<List<TeacherTimetableResponseDto>> getTimetableByClassId(@PathVariable Long classId) {
        List<TeacherTimetableResponseDto> response = timetableService.getTimetableByClassId(classId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get timetable by student")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'PARENT', 'STUDENT')")
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<TeacherTimetableResponseDto>> getTimetableByStudentId(@PathVariable Long studentId) {
        return ResponseEntity.ok(timetableService.getTimetableByStudentId(studentId));
    }

    @Operation(summary = "Update timetable entry")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<TeacherTimetableResponseDto> updateTimetable(@PathVariable Long id, @Valid @RequestBody TeacherTimetableRequestDto request) {
        TeacherTimetableResponseDto response = timetableService.updateTimetable(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete timetable entry")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimetable(@PathVariable Long id) {
        timetableService.deleteTimetable(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Import timetable from Excel")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PostMapping("/import")
    public ResponseEntity<TimetableImportResponseDto> importExcel(
            @RequestParam("file") MultipartFile file) {

        TimetableImportResponseDto response =
                timetableService.importFromExcel(file);

        return ResponseEntity.ok(response);
    }


}
