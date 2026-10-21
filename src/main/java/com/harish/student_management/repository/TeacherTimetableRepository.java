package com.harish.student_management.repository;

import com.harish.student_management.entity.TeacherTimetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherTimetableRepository
        extends JpaRepository<TeacherTimetable, Long> {

    @Query("""
        SELECT t
        FROM TeacherTimetable t
        WHERE t.id = :id
          AND t.teacher.school.id = :schoolId
          AND t.classEntity.school.id = :schoolId
          AND t.subject.school.id = :schoolId
          AND (
              t.section IS NULL
              OR t.section.classEntity.school.id = :schoolId
          )
        """)
    Optional<TeacherTimetable> findByIdAndSchoolId(
            @Param("id") Long id,
            @Param("schoolId") Long schoolId
    );

    @Query("""
        SELECT t
        FROM TeacherTimetable t
        WHERE t.teacher.id = :teacherId
          AND t.dayOfWeek = :dayOfWeek
          AND t.teacher.school.id = :schoolId
          AND t.classEntity.school.id = :schoolId
          AND t.subject.school.id = :schoolId
          AND (
              t.section IS NULL
              OR t.section.classEntity.school.id = :schoolId
          )
        ORDER BY t.startTime
        """)
    List<TeacherTimetable> findByTeacherIdAndDayOfWeekAndSchoolId(
            @Param("teacherId") Long teacherId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("schoolId") Long schoolId
    );

    @Query("""
        SELECT t
        FROM TeacherTimetable t
        WHERE t.classEntity.id = :classId
          AND t.teacher.school.id = :schoolId
          AND t.classEntity.school.id = :schoolId
          AND t.subject.school.id = :schoolId
          AND (
              t.section IS NULL
              OR t.section.classEntity.school.id = :schoolId
          )
        ORDER BY t.dayOfWeek, t.startTime
        """)
    List<TeacherTimetable> findByClassIdAndSchoolId(
            @Param("classId") Long classId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
        SELECT t
        FROM TeacherTimetable t
        WHERE t.classEntity.id = :classId
          AND (
              (:sectionId IS NULL AND t.section IS NULL)
              OR
              (:sectionId IS NOT NULL AND t.section.id = :sectionId)
          )
          AND t.dayOfWeek = :dayOfWeek
          AND t.startTime = :startTime
          AND t.endTime = :endTime
          AND t.teacher.school.id = :schoolId
          AND t.classEntity.school.id = :schoolId
          AND t.subject.school.id = :schoolId
          AND (
              t.section IS NULL
              OR t.section.classEntity.school.id = :schoolId
          )
        """)
    List<TeacherTimetable> findExistingSlot(
            @Param("classId") Long classId,
            @Param("sectionId") Long sectionId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("schoolId") Long schoolId
    );

    @Query("""
        SELECT t
        FROM TeacherTimetable t
        WHERE t.teacher.id = :teacherId
          AND t.dayOfWeek = :dayOfWeek
          AND t.startTime < :endTime
          AND t.endTime > :startTime
          AND t.teacher.school.id = :schoolId
          AND t.classEntity.school.id = :schoolId
          AND t.subject.school.id = :schoolId
          AND (
              t.section IS NULL
              OR t.section.classEntity.school.id = :schoolId
          )
        """)
    List<TeacherTimetable> findTeacherConflicts(
            @Param("teacherId") Long teacherId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("schoolId") Long schoolId
    );

    @Query("""
        SELECT t
        FROM TeacherTimetable t
        WHERE t.classEntity.id = :classId
          AND (
              (:sectionId IS NULL AND t.section IS NULL)
              OR
              (:sectionId IS NOT NULL
                  AND (t.section IS NULL OR t.section.id = :sectionId))
          )
          AND t.dayOfWeek = :dayOfWeek
          AND t.startTime < :endTime
          AND t.endTime > :startTime
          AND t.teacher.school.id = :schoolId
          AND t.classEntity.school.id = :schoolId
          AND t.subject.school.id = :schoolId
          AND (
              t.section IS NULL
              OR t.section.classEntity.school.id = :schoolId
          )
        """)
    List<TeacherTimetable> findClassConflicts(
            @Param("classId") Long classId,
            @Param("sectionId") Long sectionId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("schoolId") Long schoolId
    );

    @Query("""
        SELECT t
        FROM TeacherTimetable t
        WHERE t.teacher.id = :teacherId
          AND t.teacher.school.id = :schoolId
          AND t.classEntity.school.id = :schoolId
          AND t.subject.school.id = :schoolId
          AND (
              t.section IS NULL
              OR t.section.classEntity.school.id = :schoolId
          )
        ORDER BY t.dayOfWeek, t.startTime
        """)
    List<TeacherTimetable> findByTeacherIdAndSchoolId(
            @Param("teacherId") Long teacherId,
            @Param("schoolId") Long schoolId
    );
}