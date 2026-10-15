package com.harish.student_management.service;

import com.harish.student_management.dto.AcademicYearImportResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface AcademicYearExcelImportService {

    AcademicYearImportResponseDto importAcademicYearsExcel(
            MultipartFile file
    );
}