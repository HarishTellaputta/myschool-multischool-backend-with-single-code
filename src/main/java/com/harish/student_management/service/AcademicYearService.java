package com.harish.student_management.service;

import com.harish.student_management.dto.AcademicYearRequestDto;
import com.harish.student_management.dto.AcademicYearResponseDto;

import java.util.List;
import java.util.Optional;

public interface AcademicYearService {

    AcademicYearResponseDto createAcademicYear(AcademicYearRequestDto request);

    AcademicYearResponseDto getAcademicYearById(Long id);

    List<AcademicYearResponseDto> getAllAcademicYears();

    Optional<AcademicYearResponseDto> getCurrentAcademicYear();

    AcademicYearResponseDto updateAcademicYear(Long id, AcademicYearRequestDto request);

    void deleteAcademicYear(Long id);

    AcademicYearResponseDto setAcademicYearCurrent(
            Long id, boolean current);
}
