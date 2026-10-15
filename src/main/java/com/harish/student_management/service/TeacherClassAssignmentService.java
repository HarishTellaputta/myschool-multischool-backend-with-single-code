package com.harish.student_management.service;

import com.harish.student_management.dto.TeacherClassAssignmentRequestDto;
import com.harish.student_management.dto.TeacherClassAssignmentResponseDto;

import java.util.List;
import java.util.Set;

public interface TeacherClassAssignmentService {

    TeacherClassAssignmentResponseDto assignTeacherToClass(
            TeacherClassAssignmentRequestDto request
    );

    TeacherClassAssignmentResponseDto getAssignmentById(
            Long id
    );

    List<TeacherClassAssignmentResponseDto> getAssignmentsByTeacherId(
            Long teacherId
    );

    List<TeacherClassAssignmentResponseDto> getAssignmentsByClassId(
            Long classId
    );

    List<TeacherClassAssignmentResponseDto>
    getAssignmentsByTeacherIdAndSubject(
            Long teacherId,
            Long subjectId
    );

    Set<String> getSubjectsByTeacherId(
            Long teacherId
    );

    void removeAssignment(
            Long id
    );
}