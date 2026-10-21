package com.harish.student_management.repository;

import com.harish.student_management.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    Optional<ExamResult> findByExamScheduleIdAndStudentId(Long examScheduleId, Long studentId);
    List<ExamResult> findByExamScheduleId(Long examScheduleId);
    List<ExamResult> findByStudentId(Long studentId);
    Page<ExamResult> findByStudentId(Long studentId, Pageable pageable);
    Page<ExamResult> findByStudentIdAndIsPublishedTrue(Long studentId, Pageable pageable);
    List<ExamResult> findByExamScheduleIdOrderByPercentageDesc(
            Long examScheduleId
    );


    // NEW: Load a result only if its schedule belongs to this school.
    @Query("""
        SELECT r FROM ExamResult r
        WHERE r.id = :resultId
          AND r.examSchedule.examination.academicYear.school.id = :schoolId
        """)
    Optional<ExamResult> findByIdAndSchoolId(
            @Param("resultId") Long resultId,
            @Param("schoolId") Long schoolId);

    // NEW: Find a student's result only within a school's schedule.
    @Query("""
        SELECT r FROM ExamResult r
        WHERE r.examSchedule.id = :scheduleId
          AND r.student.id = :studentId
          AND r.examSchedule.examination.academicYear.school.id = :schoolId
        """)
    Optional<ExamResult> findByScheduleStudentAndSchool(
            @Param("scheduleId") Long scheduleId,
            @Param("studentId") Long studentId,
            @Param("schoolId") Long schoolId);

    // NEW: Results for one authorized schedule.
    @Query("""
        SELECT r FROM ExamResult r
        WHERE r.examSchedule.id = :scheduleId
          AND r.examSchedule.examination.academicYear.school.id = :schoolId
        ORDER BY r.id
        """)
    List<ExamResult> findByScheduleAndSchool(
            @Param("scheduleId") Long scheduleId,
            @Param("schoolId") Long schoolId);

    // NEW: Ranked results, scoped to the schedule's school.
    @Query("""
        SELECT r FROM ExamResult r
        WHERE r.examSchedule.id = :scheduleId
          AND r.examSchedule.examination.academicYear.school.id = :schoolId
        ORDER BY r.percentage DESC, r.marksObtained DESC, r.id ASC
        """)
    List<ExamResult> findRankedResultsByScheduleAndSchool(
            @Param("scheduleId") Long scheduleId,
            @Param("schoolId") Long schoolId);

    // NEW: All results for a student, only from this school.
    @Query("""
        SELECT r FROM ExamResult r
        WHERE r.student.id = :studentId
          AND r.examSchedule.examination.academicYear.school.id = :schoolId
        ORDER BY r.id DESC
        """)
    List<ExamResult> findByStudentAndSchool(
            @Param("studentId") Long studentId,
            @Param("schoolId") Long schoolId);

    // NEW: Paginated student performance report, scoped to the school.
    @Query("""
        SELECT r FROM ExamResult r
        WHERE r.examSchedule.examination.academicYear.school.id = :schoolId
        ORDER BY r.id DESC
        """)
    Page<ExamResult> findAllBySchool(
            @Param("schoolId") Long schoolId,
            Pageable pageable);

    // NEW: Paginated student report, scoped to both school and student.
    @Query("""
        SELECT r FROM ExamResult r
        WHERE r.student.id = :studentId
          AND r.examSchedule.examination.academicYear.school.id = :schoolId
        ORDER BY r.id DESC
        """)
    Page<ExamResult> findStudentReportBySchool(
            @Param("studentId") Long studentId,
            @Param("schoolId") Long schoolId,
            Pageable pageable);

    // NEW: Published results for a student, scoped to the school.
    @Query("""
        SELECT r FROM ExamResult r
        WHERE r.student.id = :studentId
          AND r.isPublished = true
          AND r.examSchedule.examination.academicYear.school.id = :schoolId
        ORDER BY r.id DESC
        """)
    Page<ExamResult> findPublishedByStudentAndSchool(
            @Param("studentId") Long studentId,
            @Param("schoolId") Long schoolId,
            Pageable pageable);

    @Query("""
    SELECT r
    FROM ExamResult r
    WHERE r.examSchedule.id = :scheduleId
      AND r.examSchedule.examination.academicYear.school.id = :schoolId
      AND r.examSchedule.classEntity.school.id = :schoolId
      AND r.examSchedule.subject.school.id = :schoolId
      AND r.student.section.classEntity.school.id = :schoolId
      AND r.student.academicYear.school.id = :schoolId
    ORDER BY r.id
    """)
    List<ExamResult> findPerformanceResultsByScheduleAndSchool(
            @Param("scheduleId") Long scheduleId,
            @Param("schoolId") Long schoolId
    );




}
