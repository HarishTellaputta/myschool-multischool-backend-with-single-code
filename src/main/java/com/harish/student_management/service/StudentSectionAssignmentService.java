package com.harish.student_management.service;

import com.harish.student_management.dto.StudentSectionAssignmentRequestDto;
import com.harish.student_management.dto.StudentSectionAssignmentResponseDto;

import java.util.List;

public interface StudentSectionAssignmentService {

    StudentSectionAssignmentResponseDto assignStudentToSection(StudentSectionAssignmentRequestDto request);

    StudentSectionAssignmentResponseDto getAssignmentById(Long id);

    List<StudentSectionAssignmentResponseDto> getAssignmentsByStudentId(Long studentId);

    List<StudentSectionAssignmentResponseDto> getAssignmentsBySectionId(Long sectionId);

    List<StudentSectionAssignmentResponseDto> getAssignmentsByAcademicYearId(Long academicYearId);

    List<StudentSectionAssignmentResponseDto> getAssignmentsBySectionAndYear(Long sectionId, Long academicYearId);

    void removeAssignment(Long id);
}
