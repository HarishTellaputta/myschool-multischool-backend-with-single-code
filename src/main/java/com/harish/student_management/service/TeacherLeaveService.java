package com.harish.student_management.service;

import com.harish.student_management.dto.TeacherLeaveApprovalDto;
import com.harish.student_management.dto.TeacherLeaveRequestDto;
import com.harish.student_management.dto.TeacherLeaveResponseDto;

import java.util.List;

public interface TeacherLeaveService {

    TeacherLeaveResponseDto applyForLeave(TeacherLeaveRequestDto request);

    TeacherLeaveResponseDto getLeaveById(Long id);

    List<TeacherLeaveResponseDto> getLeavesByTeacherId(Long teacherId);

    List<TeacherLeaveResponseDto> getLeavesByTeacherIdAndStatus(Long teacherId, String status);

    List<TeacherLeaveResponseDto> getLeavesByStatus(String status);

    TeacherLeaveResponseDto approveLeave(Long id, TeacherLeaveApprovalDto approval, String approvedBy);

    TeacherLeaveResponseDto updateLeave(Long id, TeacherLeaveRequestDto request);

    void deleteLeave(Long id);
}
