
package com.harish.student_management.repository;

import com.harish.student_management.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {

    List<ExamSchedule> findByExaminationId(Long examinationId);

    List<ExamSchedule> findByClassEntityId(Long classId);

    List<ExamSchedule> findBySectionId(Long sectionId);

    List<ExamSchedule> findTop10ByExamDateGreaterThanEqualOrderByExamDateAsc(
            LocalDate date
    );

    Page<ExamSchedule> findByExamDateBetween(
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    Page<ExamSchedule> findByClassEntityId(
            Long classId,
            Pageable pageable
    );

    List<ExamSchedule> findByClassEntityIdAndSubjectIdOrderByExamDateDescIdDesc(
            Long classId,
            Long subjectId
    );

    // =========================================================
    // GET ALL EXAM SCHEDULES FOR A SUBJECT
    // =========================================================

    List<ExamSchedule> findBySubjectIdOrderByExamDateDescIdDesc(
            Long subjectId
    );

    @Query("""
                SELECT s
                FROM ExamSchedule s
                WHERE s.examination.id = :examinationId
                  AND s.classEntity.id = :classId
                  AND s.subject.id = :subjectId
                  AND s.examDate = :examDate
                  AND (
                      (:sectionId IS NULL AND s.section IS NULL)
                      OR
                      (:sectionId IS NOT NULL AND s.section.id = :sectionId)
                  )
            """)
    Optional<ExamSchedule> findExistingSchedule(
            @Param("examinationId") Long examinationId,
            @Param("classId") Long classId,
            @Param("sectionId") Long sectionId,
            @Param("subjectId") Long subjectId,
            @Param("examDate") LocalDate examDate
    );


    @Query("""
                SELECT s
                FROM ExamSchedule s
                WHERE s.classEntity.id = :classId
                  AND s.subject.id = :subjectId
                  AND (
                      (:sectionId IS NULL AND s.section IS NULL)
                      OR
                      (:sectionId IS NOT NULL AND s.section.id = :sectionId)
                  )
                ORDER BY s.examDate DESC, s.id DESC
            """)
    List<ExamSchedule> findByClassSectionSubjectOrderByLatest(
            @Param("classId") Long classId,
            @Param("sectionId") Long sectionId,
            @Param("subjectId") Long subjectId
    );


    @Query("""
            SELECT s FROM ExamSchedule s
            WHERE s.examination.id = :examinationId
              AND s.classEntity.id = :classId
              AND s.subject.id = :subjectId
              AND s.examDate = :examDate
              AND (
                (:sectionId IS NULL AND s.section IS NULL)
                OR (:sectionId IS NOT NULL AND s.section.id = :sectionId)
              )
            """)
    Optional<ExamSchedule> findExistingSchedule(
            @Param("examinationId") Long examinationId,
            @Param("classId") Long classId,
            @Param("subjectId") Long subjectId,
            @Param("examDate") LocalDate examDate,
            @Param("sectionId") Long sectionId);


    // NEW: Find one schedule only if it belongs to the given school.
    @Query("""
            SELECT s FROM ExamSchedule s
            WHERE s.id = :scheduleId
              AND s.examination.academicYear.school.id = :schoolId
            """)
    Optional<ExamSchedule> findByIdAndSchoolId(
            @Param("scheduleId") Long scheduleId,
            @Param("schoolId") Long schoolId);

    // NEW: School-scoped date report, with database pagination.
    @Query("""
            SELECT s FROM ExamSchedule s
            WHERE s.examination.academicYear.school.id = :schoolId
              AND s.examDate BETWEEN :fromDate AND :toDate
            ORDER BY s.examDate DESC, s.id DESC
            """)
    Page<ExamSchedule> findReportBySchoolAndExamDateBetween(
            @Param("schoolId") Long schoolId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    // NEW: School-scoped class report, with database pagination.
    @Query("""
            SELECT s FROM ExamSchedule s
            WHERE s.examination.academicYear.school.id = :schoolId
              AND s.classEntity.id = :classId
            ORDER BY s.examDate DESC, s.id DESC
            """)
    Page<ExamSchedule> findReportBySchoolAndClassId(
            @Param("schoolId") Long schoolId,
            @Param("classId") Long classId,
            Pageable pageable);

    @Query("""
    SELECT s
    FROM ExamSchedule s
    WHERE s.id = :scheduleId
      AND s.examination.academicYear.school.id = :schoolId
      AND s.classEntity.school.id = :schoolId
      AND s.subject.school.id = :schoolId
      AND (
          (s.section IS NULL AND :sectionId IS NULL)
          OR s.section.id = :sectionId
      )
    """)
    Optional<ExamSchedule> findPerformanceScheduleByIdAndSchool(
            @Param("scheduleId") Long scheduleId,
            @Param("schoolId") Long schoolId,
            @Param("sectionId") Long sectionId
    );




    @Query("""
    SELECT s
    FROM ExamSchedule s
    WHERE s.examination.academicYear.school.id = :schoolId
      AND s.classEntity.school.id = :schoolId
      AND s.subject.school.id = :schoolId
      AND s.classEntity.id = :classId
      AND s.subject.id = :subjectId
      AND (
          (:sectionId IS NULL AND s.section IS NULL)
          OR
          (:sectionId IS NOT NULL AND s.section.id = :sectionId)
      )
    ORDER BY s.examDate DESC, s.id DESC
    """)
    List<ExamSchedule> findPerformanceSchedulesByClassSectionSubjectAndSchool(
            @Param("classId") Long classId,
            @Param("sectionId") Long sectionId,
            @Param("subjectId") Long subjectId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
    SELECT s
    FROM ExamSchedule s
    WHERE s.examination.academicYear.school.id = :schoolId
      AND s.classEntity.school.id = :schoolId
      AND s.subject.school.id = :schoolId
      AND s.subject.id = :subjectId
    ORDER BY s.examDate DESC, s.id DESC
    """)
    List<ExamSchedule> findPerformanceSchedulesBySubjectAndSchool(
            @Param("subjectId") Long subjectId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
    SELECT s
    FROM ExamSchedule s
    LEFT JOIN s.section sec
    WHERE s.examination.academicYear.school.id = :schoolId
      AND s.classEntity.school.id = :schoolId
      AND s.subject.school.id = :schoolId
      AND (:examinationId IS NULL
           OR s.examination.id = :examinationId)
      AND (:classId IS NULL
           OR s.classEntity.id = :classId)
      AND (:sectionId IS NULL
           OR sec.id = :sectionId)
      AND (:subjectId IS NULL
           OR s.subject.id = :subjectId)
    ORDER BY s.examDate DESC, s.id DESC
    """)
    List<ExamSchedule> findSchedulesForSchool(
            @Param("schoolId") Long schoolId,
            @Param("examinationId") Long examinationId,
            @Param("classId") Long classId,
            @Param("sectionId") Long sectionId,
            @Param("subjectId") Long subjectId
    );


}

