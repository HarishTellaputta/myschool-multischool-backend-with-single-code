package com.harish.student_management.service;

import com.harish.student_management.dto.CreateSchoolWithAdminRequestDto;
import com.harish.student_management.dto.SchoolImportResponseDto;
import com.harish.student_management.dto.SchoolRequestDto;
import com.harish.student_management.dto.SchoolResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface SchoolService {

    SchoolResponseDto createSchool(SchoolRequestDto request);

    SchoolResponseDto getSchoolById(Long id);

    List<SchoolResponseDto> getAllSchools();

    SchoolResponseDto updateSchool(Long id, SchoolRequestDto request);

    void deleteSchool(Long id);

    SchoolResponseDto uploadSchoolLogo(Long id, MultipartFile file) throws IOException;

    SchoolImportResponseDto importSchools(MultipartFile file) throws IOException;

    SchoolResponseDto createSchoolWithAdmin(
            CreateSchoolWithAdminRequestDto request);

    SchoolResponseDto updateSchoolStatus(Long id, String status);
}
