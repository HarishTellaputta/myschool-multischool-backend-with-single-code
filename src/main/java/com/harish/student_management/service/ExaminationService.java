
        package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface ExaminationService {

    ExaminationResponseDto createExamination(
            ExaminationRequestDto request
    );


    ExaminationResponseDto updateExamination(
            Long id,
            ExaminationRequestDto request
    );

    List<ExaminationResponseDto> getExaminations(
            Long academicYearId
    );

    ExamScheduleResponseDto createSchedule(
            ExamScheduleRequestDto request
    );

    ExamScheduleResponseDto updateSchedule(
            Long id,
            ExamScheduleRequestDto request
    );

    List<ExamScheduleResponseDto> getSchedules(
            Long examinationId,
            Long classId,
            Long sectionId,
            Long subjectId
    );

    ExamResultResponseDto enterMarks(
            ExamResultRequestDto request,
            String username,
            boolean admin
    );

    List<ExamResultResponseDto> getResults(
            Long scheduleId,
            Long studentId
    );

    ExamResultResponseDto setResultPublication(
            Long resultId,
            boolean published
    );

//    // Excel import
//    ExaminationImportResponseDto importExaminationsExcel(
//            MultipartFile file
//    );
//
//    ExamResultImportResponseDto importExamResultsExcel(MultipartFile file);

    GradeRuleImportResponseDto importGradeRulesExcel(MultipartFile file);
//
//    ExamScheduleImportResponseDto importExamSchedulesExcel(MultipartFile file);


    Page<ExamScheduleResponseDto> getExamScheduleReport(
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    Page<ExamResultResponseDto> getAcademicPerformanceReport(
            Pageable pageable
    );

    Page<ExamResultResponseDto> getStudentPerformanceReport(
            Long studentId,
            Pageable pageable,
            boolean staff
    );

    Page<ExamScheduleResponseDto> getTeacherClassPerformanceReport(
            Long classId,
            Pageable pageable
    );


    List<ExamResultResponseDto> setStudentResultsPublication(
            Long studentId,
            Long examinationId,
            boolean published
    );

    List<ExamResultResponseDto> setExaminationResultsPublication(
            Long examinationId,
            boolean published
    );

}
