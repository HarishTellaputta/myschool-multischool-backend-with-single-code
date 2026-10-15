package com.harish.student_management.service;

import com.harish.student_management.dto.AssignmentRequestDto;
import com.harish.student_management.dto.AssignmentResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface AssignmentService {

    AssignmentResponseDto createAssignment(
            AssignmentRequestDto request
    );

    AssignmentResponseDto getAssignmentById(
            Long id
    );

    List<AssignmentResponseDto> getAllAssignments();

    List<AssignmentResponseDto> getAssignmentsByClassId(
            Long classId
    );

    List<AssignmentResponseDto> getAssignmentsBySectionId(
            Long sectionId
    );

    List<AssignmentResponseDto> getAssignmentsByClassIdAndSectionId(
            Long classId,
            Long sectionId
    );

    List<AssignmentResponseDto> getAssignmentsByTeacherId(
            Long teacherId
    );

    List<AssignmentResponseDto> getAssignmentsBySubject(
            Long subjectId
    );

    List<AssignmentResponseDto> getAssignmentsByClassIdAndDueDateRange(
            Long classId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<AssignmentResponseDto> getActiveAssignmentsByClassId(
            Long classId
    );

    List<AssignmentResponseDto> getAssignmentsByClassIdAndSubject(
            Long classId,
            Long subjectId
    );

    List<AssignmentResponseDto> getAssignmentsByStatus(
            String status
    );

    AssignmentResponseDto updateAssignment(
            Long id,
            AssignmentRequestDto request
    );

    void deleteAssignment(
            Long id
    );
}