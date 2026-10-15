package com.harish.student_management.service;

import com.harish.student_management.dto.FileUploadResponseDto;
import com.harish.student_management.dto.StudentGenderSummaryDto;
import com.harish.student_management.dto.StudentRequestDto;
import com.harish.student_management.dto.StudentResponseDto;
import com.harish.student_management.entity.Student;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

public interface StudentService {

    StudentResponseDto saveStudent(StudentRequestDto student);

    Page<StudentResponseDto> getAllStudents(int page, int size, String sortBy, String sortDirection);

    StudentResponseDto getStudentById(Long id);

    StudentResponseDto updateStudent(Long id, StudentRequestDto student);

    void deleteStudent(Long id);

    FileUploadResponseDto uploadFile(MultipartFile file) throws IOException;
    Resource downloadFile(String fileName) throws MalformedURLException;

    String deleteFile(String fileName) throws IOException;
    List<StudentResponseDto> getStudentsByClassId(Long classId);
    List<StudentResponseDto> getStudentsBySectionId(Long sectionId);
    StudentGenderSummaryDto getStudentGenderSummary();

}
