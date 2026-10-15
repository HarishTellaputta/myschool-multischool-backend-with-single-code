package com.harish.student_management.service;

import com.harish.student_management.dto.SubjectPerformanceResponseDto;
import com.harish.student_management.dto.TeacherPerformanceResponseDto;

import java.util.List;

public interface TeacherPerformanceService {

    // =========================================================
    // ADMIN
    // Selected teacher performance
    // =========================================================
    List<TeacherPerformanceResponseDto> getTeacherPerformance(
            Long teacherId
    );

    // =========================================================
    // TEACHER
    // Logged-in teacher's own latest performance
    // =========================================================
    List<TeacherPerformanceResponseDto> getMyPerformance(
            String username
    );

    // =========================================================
    // ADMIN
    // Subject performance history
    // =========================================================
    List<SubjectPerformanceResponseDto> getSubjectPerformance(
            Long subjectId
    );

    // =========================================================
    // TEACHER
    // Logged-in teacher's subject-wise performance history
    // =========================================================
    List<SubjectPerformanceResponseDto> getMySubjectPerformance(
            String username
    );
}