
        package com.harish.student_management.controller;

import com.harish.student_management.dto.*;
import com.harish.student_management.entity.GradeRule;
import com.harish.student_management.repository.GradeRuleRepository;
import com.harish.student_management.service.CurrentSchoolService;
import com.harish.student_management.service.ExamImportService;
import com.harish.student_management.service.ExaminationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/examinations")
@RequiredArgsConstructor
public class ExaminationController {

    private final ExaminationService examinationService;
    private final GradeRuleRepository gradeRuleRepository;
    private final ExamImportService examImportService;
    private final CurrentSchoolService currentSchoolService;

    // =========================================================
    // EXAMINATIONS
    // =========================================================

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ExaminationResponseDto> create(
            @Valid @RequestBody ExaminationRequestDto r) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examinationService.createExamination(r));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ExaminationResponseDto update(
            @PathVariable Long id,
            @Valid @RequestBody ExaminationRequestDto r) {

        return examinationService.updateExamination(id, r);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER','PARENT','STUDENT')")
    public List<ExaminationResponseDto> list(
            @RequestParam(required = false) Long academicYearId) {

        return examinationService.getExaminations(academicYearId);
    }

    // =========================================================
    // EXAMINATION EXCEL IMPORT
    // =========================================================

    @PostMapping("/import")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ExaminationImportResponseDto> importExaminationsExcel(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                examImportService.importExaminationsExcel(file)
        );
    }

    // =========================================================
    // EXAM SCHEDULES
    // =========================================================

    @PostMapping("/schedules")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ExamScheduleResponseDto> createSchedule(
            @Valid @RequestBody ExamScheduleRequestDto r) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examinationService.createSchedule(r));
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ExamScheduleResponseDto updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody ExamScheduleRequestDto r) {

        return examinationService.updateSchedule(id, r);
    }

    @GetMapping("/schedules")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER','PARENT','STUDENT')")
    public List<ExamScheduleResponseDto> schedules(
            @RequestParam(required = false) Long examinationId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long subjectId) {

        return examinationService.getSchedules(
                examinationId,
                classId,
                sectionId,
                subjectId
        );
    }

    // =========================================================
    // EXAM RESULTS
    // =========================================================

    @PostMapping("/results")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER')")
    public ResponseEntity<ExamResultResponseDto> enterMarks(
            @Valid @RequestBody ExamResultRequestDto r,
            Authentication auth) {

        boolean admin = auth.getAuthorities()
                .stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_SCHOOL_ADMIN"));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examinationService.enterMarks(
                        r,
                        auth.getName(),
                        admin
                ));
    }

    @GetMapping("/results")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER','PARENT','STUDENT')")
    public List<ExamResultResponseDto> results(
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) Long studentId,
            Authentication auth) {

        List<ExamResultResponseDto> results =
                examinationService.getResults(scheduleId, studentId);

        boolean staff = auth.getAuthorities()
                .stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_SCHOOL_ADMIN")
                                || a.getAuthority().equals("ROLE_TEACHER"));

        return staff
                ? results
                : results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsPublished()))
                .toList();
    }

    @PatchMapping("/results/{id}/publish")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ExamResultResponseDto publish(
            @PathVariable Long id,
            @RequestParam boolean published) {

        return examinationService.setResultPublication(id, published);
    }

    // =========================================================
    // GRADE RULES
    // =========================================================

    @GetMapping("/grade-rules")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','TEACHER','PARENT','STUDENT')")
    public List<GradeRule> gradeRules() {
        Long schoolId = currentSchoolService.getCurrentSchoolId();

        return gradeRuleRepository
                .findAllBySchool_IdOrderByMinimumPercentageDesc(schoolId);
    }

    @PostMapping("/grade-rules")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public GradeRule addGradeRule(
            @Valid @RequestBody GradeRuleRequestDto r) {

        Long schoolId = currentSchoolService.getCurrentSchoolId();

        if (r.getMinimumPercentage() == null
                || r.getMaximumPercentage() == null
                || r.getMinimumPercentage() < 0
                || r.getMaximumPercentage() > 100
                || r.getMinimumPercentage() > r.getMaximumPercentage()) {
            throw new IllegalArgumentException(
                    "Grade percentage range must be valid and between 0 and 100"
            );
        }

        List<GradeRule> existingRules =
                gradeRuleRepository.findAllBySchool_Id(schoolId);

        boolean overlaps = existingRules.stream()
                .anyMatch(existing ->
                        r.getMinimumPercentage()
                                <= existing.getMaximumPercentage()
                                && r.getMaximumPercentage()
                                >= existing.getMinimumPercentage()
                );

        if (overlaps) {
            throw new IllegalArgumentException(
                    "Grade percentage range overlaps an existing rule"
            );
        }

        GradeRule rule = GradeRule.builder()
                .grade(r.getGrade())
                .minimumPercentage(r.getMinimumPercentage())
                .maximumPercentage(r.getMaximumPercentage())
                .school(
                        currentSchoolService.getCurrentSchool()
                )
                .build();

        return gradeRuleRepository.save(rule);
    }

    @PostMapping("/results/import")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ExamResultImportResponseDto> importExamResultsExcel(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                examImportService.importExamResultsExcel(file)
        );
    }

    @PostMapping("/grade-rules/import")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<GradeRuleImportResponseDto> importGradeRulesExcel(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                examinationService.importGradeRulesExcel(file)
        );
    }


    @PostMapping("/schedules/import")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ExamScheduleImportResponseDto> importExamSchedulesExcel(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                examImportService.importExamSchedulesExcel(file)
        );
    }


    @PatchMapping("/students/{studentId}/results/publish")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<ExamResultResponseDto> publishStudentResults(
            @PathVariable Long studentId,
            @RequestParam Long examinationId,
            @RequestParam boolean published) {

        return examinationService.setStudentResultsPublication(
                studentId,
                examinationId,
                published
        );
    }

    @PatchMapping("/{examinationId}/results/publish")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public List<ExamResultResponseDto> publishExaminationResults(
            @PathVariable Long examinationId,
            @RequestParam boolean published) {

        return examinationService.setExaminationResultsPublication(
                examinationId,
                published
        );
    }


}

