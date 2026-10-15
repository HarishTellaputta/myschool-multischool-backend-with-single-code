package com.harish.student_management.service;

import com.harish.student_management.dto.AvailableTeacherUserResponseDto;
import com.harish.student_management.dto.TeacherRequestDto;
import com.harish.student_management.dto.TeacherResponseDto;

import java.util.List;

public interface TeacherService {

    TeacherResponseDto createTeacher(TeacherRequestDto request);

    TeacherResponseDto getTeacherById(Long id);
    TeacherResponseDto getTeacherByUserId(Long userId);

    List<TeacherResponseDto> getAllTeachers();

    List<TeacherResponseDto> getTeachersBySchoolId(Long schoolId);

    List<TeacherResponseDto> getTeachersByStatus(String status);

    TeacherResponseDto updateTeacher(Long id, TeacherRequestDto request);

    void deleteTeacher(Long id);
    List<AvailableTeacherUserResponseDto> getAvailableTeacherUsers();
}
