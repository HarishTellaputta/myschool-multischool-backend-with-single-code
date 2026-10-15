
        package com.harish.student_management.service;

import com.harish.student_management.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface McqTestService {

    McqTestResponseDto create(
            McqTestRequestDto r
    );

    List<McqTestResponseDto> list(
            LocalDate date
    );

    McqAttemptResponseDto start(
            Long testId,
            Long studentId,
            String username,
            boolean isParent
    );

    McqAttemptResponseDto answer(
            Long attemptId,
            Long studentId,
            McqAnswerRequestDto r,
            String username,
            boolean isParent
    );

    McqAttemptResponseDto submit(
            Long attemptId,
            Long studentId,
            String username,
            boolean isParent
    );

    List<McqAttemptResponseDto> history(
            Long studentId
    );

    List<McqAttemptResponseDto> classPerformance(
            Long classId
    );

    List<McqAttemptResponseDto> overallPerformance();

    Page<McqAttemptResponseDto> reportPerformance(
            LocalDate date,
            Long classId,
            Long sectionId,
            String subject,
            Pageable pageable
    );
}
