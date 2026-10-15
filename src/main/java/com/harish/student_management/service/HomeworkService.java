package com.harish.student_management.service;

import com.harish.student_management.dto.HomeworkRequestDto;
import com.harish.student_management.dto.HomeworkResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface HomeworkService {

    HomeworkResponseDto createHomework(
            HomeworkRequestDto request
    );

    HomeworkResponseDto getHomeworkById(
            Long id
    );

    List<HomeworkResponseDto> getAllHomework();

    List<HomeworkResponseDto> getHomeworkByClassId(
            Long classId
    );

    List<HomeworkResponseDto> getHomeworkBySectionId(
            Long sectionId
    );

    List<HomeworkResponseDto> getHomeworkByClassIdAndSectionId(
            Long classId,
            Long sectionId
    );

    List<HomeworkResponseDto> getHomeworkByTeacherId(
            Long teacherId
    );

    List<HomeworkResponseDto> getHomeworkBySubject(
            Long subjectId
    );

    List<HomeworkResponseDto> getHomeworkByClassIdAndDueDateRange(
            Long classId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<HomeworkResponseDto> getActiveHomeworkByClassId(
            Long classId
    );

    List<HomeworkResponseDto> getHomeworkByClassIdAndSubject(
            Long classId,
            Long subjectId
    );

    HomeworkResponseDto updateHomework(
            Long id,
            HomeworkRequestDto request
    );

    void deleteHomework(
            Long id
    );
}