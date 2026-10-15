package com.harish.student_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherTimetableResponseDto {

    private Long id;

    // =========================================================
    // TEACHER
    // =========================================================

    private Long teacherId;

    private String teacherName;


    // =========================================================
    // CLASS
    // =========================================================

    private Long classId;

    private String className;


    // =========================================================
    // SECTION
    // =========================================================

    private Long sectionId;

    private String sectionName;


    // =========================================================
    // SUBJECT MASTER
    // =========================================================

    private Long subjectId;

    private String subjectName;

    private String subjectCode;


    // =========================================================
    // TIMING
    // =========================================================

    private DayOfWeek dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;


    // =========================================================
    // ROOM
    // =========================================================

    private String roomNumber;


    // =========================================================
    // AUDIT
    // =========================================================

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}