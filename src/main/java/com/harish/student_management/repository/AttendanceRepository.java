package com.harish.student_management.repository;

import com.harish.student_management.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.*;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query("select a from Attendance a where a.student.id = :studentId and a.classEntity.id = :classId and a.attendanceDate = :attendanceDate")
    Optional<Attendance> findByStudentIdAndClassIdAndAttendanceDate(@Param("studentId") Long studentId, @Param("classId") Long classId, @Param("attendanceDate") LocalDate attendanceDate);

    List<Attendance> findByStudentIdAndAttendanceDateBetween(Long studentId, LocalDate startDate, LocalDate endDate);

    @Query("select a from Attendance a where a.classEntity.id = :classId and a.attendanceDate = :attendanceDate")
    List<Attendance> findByClassIdAndAttendanceDate(@Param("classId") Long classId, @Param("attendanceDate") LocalDate attendanceDate);

    @Query("select a from Attendance a where a.classEntity.id = :classId and a.attendanceDate between :startDate and :endDate")
    List<Attendance> findByClassIdAndAttendanceDateBetween(@Param("classId") Long classId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<Attendance> findByStudentId(Long studentId);

    @Query("select a from Attendance a where a.classEntity.id = :classId")
    List<Attendance> findByClassId(@Param("classId") Long classId);

    @Query("select a from Attendance a where a.student.id = :studentId and a.classEntity.id = :classId")
    List<Attendance> findByStudentIdAndClassId(@Param("studentId") Long studentId, @Param("classId") Long classId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.classEntity.id = :classId AND a.attendanceDate BETWEEN :startDate AND :endDate")
    long countByStudentIdAndClassIdAndDateRange(@Param("studentId") Long studentId, @Param("classId") Long classId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.classEntity.id = :classId AND a.attendanceDate BETWEEN :startDate AND :endDate AND a.status = :status")
    long countByStudentIdAndClassIdAndDateRangeAndStatus(@Param("studentId") Long studentId, @Param("classId") Long classId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("status") String status);

    @Query("SELECT a FROM Attendance a WHERE a.student.parent.id = :parentId AND a.attendanceDate BETWEEN :startDate AND :endDate")
    List<Attendance> findByParentIdAndDateRange(@Param("parentId") Long parentId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("select case when count(a) > 0 then true else false end from Attendance a where a.student.id = :studentId and a.classEntity.id = :classId and a.attendanceDate = :attendanceDate")
    boolean existsByStudentIdAndClassIdAndAttendanceDate(@Param("studentId") Long studentId, @Param("classId") Long classId, @Param("attendanceDate") LocalDate attendanceDate);
    long countByAttendanceDateAndStatus(LocalDate date, String status);
    Page<Attendance> findByAttendanceDateBetween(LocalDate from, LocalDate to, Pageable pageable);
    long countByAttendanceDate(LocalDate date);


    // =========================================================
    // SCHOOL-SCOPED DAILY ATTENDANCE QUERIES
    // =========================================================

    Optional<Attendance>
    findByIdAndClassEntity_School_Id(Long id, Long schoolId);

    List<Attendance>
    findByStudent_IdAndClassEntity_School_IdAndAttendanceDateBetween(
            Long studentId,
            Long schoolId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Attendance>
    findByClassEntity_IdAndClassEntity_School_IdAndAttendanceDate(
            Long classId,
            Long schoolId,
            LocalDate attendanceDate
    );

    List<Attendance>
    findByClassEntity_IdAndClassEntity_School_IdAndAttendanceDateBetween(
            Long classId,
            Long schoolId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Attendance>
    findByStudent_IdAndClassEntity_IdAndClassEntity_School_IdAndAttendanceDateBetween(
            Long studentId,
            Long classId,
            Long schoolId,
            LocalDate startDate,
            LocalDate endDate
    );

    boolean existsByStudent_IdAndClassEntity_IdAndClassEntity_School_IdAndAttendanceDate(
            Long studentId,
            Long classId,
            Long schoolId,
            LocalDate attendanceDate
    );

    long countByClassEntity_School_IdAndAttendanceDate(
            Long schoolId,
            LocalDate attendanceDate
    );

    long countByClassEntity_School_IdAndAttendanceDateAndStatusIgnoreCase(
            Long schoolId,
            LocalDate attendanceDate,
            String status
    );

    @Query("""
        SELECT COUNT(a)
        FROM Attendance a
        WHERE a.student.id = :studentId
          AND a.classEntity.id = :classId
          AND a.classEntity.school.id = :schoolId
          AND a.attendanceDate BETWEEN :startDate AND :endDate
        """)
    long countByStudentClassSchoolAndDateRange(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId,
            @Param("schoolId") Long schoolId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT COUNT(a)
        FROM Attendance a
        WHERE a.student.id = :studentId
          AND a.classEntity.id = :classId
          AND a.classEntity.school.id = :schoolId
          AND a.attendanceDate BETWEEN :startDate AND :endDate
          AND UPPER(a.status) = UPPER(:status)
        """)
    long countByStudentClassSchoolDateRangeAndStatus(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId,
            @Param("schoolId") Long schoolId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") String status
    );


    // School-scoped duplicate check
    Optional<Attendance>
    findByStudent_IdAndClassEntity_IdAndClassEntity_School_IdAndAttendanceDate(
            Long studentId,
            Long classId,
            Long schoolId,
            LocalDate attendanceDate
    );

    // School-scoped attendance records for a student and class
    List<Attendance>
    findByStudent_IdAndClassEntity_IdAndClassEntity_School_Id(
            Long studentId,
            Long classId,
            Long schoolId
    );

    // School-scoped attendance records for a student
    List<Attendance>
    findByStudent_IdAndClassEntity_School_Id(
            Long studentId,
            Long schoolId
    );

    // School-scoped dashboard counts
    long countByClassEntity_School_IdAndAttendanceDateAndStatus(
            Long schoolId,
            LocalDate date,
            String status
    );

    // School-scoped attendance pagination
    Page<Attendance>
    findByClassEntity_School_IdAndAttendanceDateBetween(
            Long schoolId,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    @Query("""
    SELECT a
    FROM Attendance a
    JOIN a.student s
    JOIN s.parent p
    JOIN a.classEntity c
    WHERE p.id = :parentId
      AND c.school.id = :schoolId
      AND a.attendanceDate BETWEEN :startDate AND :endDate
    ORDER BY a.attendanceDate DESC
    """)
    List<Attendance> findParentAttendanceByParentAndSchoolAndDateRange(
            @Param("parentId") Long parentId,
            @Param("schoolId") Long schoolId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );



    boolean existsByStudent_IdAndClassEntity_IdAndClassEntity_School_IdAndAttendanceDateAndPeriodNumber(
            Long studentId,
            Long classId,
            Long schoolId,
            LocalDate attendanceDate,
            Integer periodNumber
    );



}
