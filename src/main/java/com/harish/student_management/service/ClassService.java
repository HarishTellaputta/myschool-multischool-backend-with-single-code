package com.harish.student_management.service;

import com.harish.student_management.dto.ClassImportResponseDto;
import com.harish.student_management.dto.ClassRequestDto;
import com.harish.student_management.dto.ClassResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ClassService {

    ClassResponseDto createClass(ClassRequestDto request);

    ClassResponseDto getClassById(Long id);

    List<ClassResponseDto> getAllClasses();

    List<ClassResponseDto> getClassesBySchoolId(Long schoolId);

    ClassResponseDto updateClass(Long id, ClassRequestDto request);

    void deleteClass(Long id);

    ClassImportResponseDto importClasses(
            MultipartFile file,
            Long schoolId
    ) throws IOException;
}