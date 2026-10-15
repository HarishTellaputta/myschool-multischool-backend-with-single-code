package com.harish.student_management.service;

import com.harish.student_management.dto.McqQuestionRequestDto;
import com.harish.student_management.dto.McqQuestionResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface McqQuestionService {

    McqQuestionResponseDto create(
            McqQuestionRequestDto request,
            String username,
            boolean admin
    );

    McqQuestionResponseDto update(
            Long id,
            McqQuestionRequestDto request,
            String username,
            boolean admin
    );

    McqQuestionResponseDto get(Long id);

    List<McqQuestionResponseDto> list(
            LocalDate date,
            Long classId,
            Long subjectId
    );

    void delete(
            Long id,
            String username,
            boolean admin
    );
}