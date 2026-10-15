package com.harish.student_management.service;

import com.harish.student_management.dto.TeacherTimetableRequestDto;
import com.harish.student_management.dto.TeacherTimetableResponseDto;
import com.harish.student_management.dto.TimetableImportResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TeacherTimetableService {

    TeacherTimetableResponseDto createTimetableEntry(
            TeacherTimetableRequestDto request
    );

    TeacherTimetableResponseDto getTimetableById(
            Long id
    );

    List<TeacherTimetableResponseDto> getTimetableByTeacherId(
            Long teacherId
    );

    List<TeacherTimetableResponseDto> getTimetableByTeacherIdAndDayOfWeek(
            Long teacherId,
            String dayOfWeek
    );

    List<TeacherTimetableResponseDto> getTimetableByClassId(
            Long classId
    );

    List<TeacherTimetableResponseDto> getTimetableByStudentId(
            Long studentId
    );

    TeacherTimetableResponseDto updateTimetable(
            Long id,
            TeacherTimetableRequestDto request
    );

    void deleteTimetable(
            Long id
    );
    TimetableImportResponseDto importFromExcel(MultipartFile file);
}