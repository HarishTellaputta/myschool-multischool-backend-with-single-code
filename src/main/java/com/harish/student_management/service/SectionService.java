package com.harish.student_management.service;

import com.harish.student_management.dto.SectionImportResponseDto;
import com.harish.student_management.dto.SectionRequestDto;
import com.harish.student_management.dto.SectionResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface SectionService {

    SectionResponseDto createSection(SectionRequestDto request);

    SectionResponseDto getSectionById(Long id);

    List<SectionResponseDto> getAllSections();

    List<SectionResponseDto> getSectionsByClassId(Long classId);

    SectionResponseDto updateSection(Long id, SectionRequestDto request);

    void deleteSection(Long id);

    SectionImportResponseDto importSections(
            MultipartFile file,
            Long schoolId
    ) throws IOException;
}