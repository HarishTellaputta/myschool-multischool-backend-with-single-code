package com.harish.student_management.service;

import com.harish.student_management.dto.ExamResultImportResponseDto;
import com.harish.student_management.dto.ExamScheduleImportResponseDto;
import com.harish.student_management.dto.ExaminationImportResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface ExamImportService {

        ExaminationImportResponseDto importExaminationsExcel(MultipartFile file);

        ExamScheduleImportResponseDto importExamSchedulesExcel(MultipartFile file);

        ExamResultImportResponseDto importExamResultsExcel(MultipartFile file);

}