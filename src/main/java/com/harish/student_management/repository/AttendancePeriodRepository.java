
package com.harish.student_management.repository;

import com.harish.student_management.entity.AttendancePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendancePeriodRepository
        extends JpaRepository<AttendancePeriod, Long> {

    // Find one student's attendance for a specific period and date
    Optional<AttendancePeriod>
    findByStudent_IdAndClassEntity_IdAndAttendanceDateAndPeriodNumber(
            Long studentId,
            Long classId,
            LocalDate attendanceDate,
            Integer periodNumber
    );


    // All periods for a student on a particular date
    List<AttendancePeriod>
    findByStudent_IdAndAttendanceDateOrderByPeriodNumberAsc(
            Long studentId,
            LocalDate attendanceDate
    );

    // All periods for a student within a date range
    List<AttendancePeriod>
    findByStudent_IdAndAttendanceDateBetweenOrderByAttendanceDateAscPeriodNumberAsc(
            Long studentId,
            LocalDate startDate,
            LocalDate endDate
    );

    // All student period records for a class on a particular date
    List<AttendancePeriod>
    findByClassEntity_IdAndAttendanceDateOrderByPeriodNumberAsc(
            Long classId,
            LocalDate attendanceDate
    );

    // All period records for a class within a date range
    List<AttendancePeriod>
    findByClassEntity_IdAndAttendanceDateBetweenOrderByAttendanceDateAscPeriodNumberAsc(
            Long classId,
            LocalDate startDate,
            LocalDate endDate
    );

    // All records for a student in a particular class and date range
    List<AttendancePeriod>
    findByStudent_IdAndClassEntity_IdAndAttendanceDateBetweenOrderByAttendanceDateAscPeriodNumberAsc(
            Long studentId,
            Long classId,
            LocalDate startDate,
            LocalDate endDate
    );

    // Count a student's period attendance by status
    @Query("""
        SELECT COUNT(ap)
        FROM AttendancePeriod ap
        WHERE ap.student.id = :studentId
          AND ap.classEntity.id = :classId
          AND ap.attendanceDate BETWEEN :startDate AND :endDate
          AND UPPER(ap.status) = UPPER(:status)
        """)
    long countByStudentAndClassAndDateRangeAndStatus(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") String status
    );

    // Count all recorded periods for a class on a date
    long countByClassEntity_IdAndAttendanceDate(
            Long classId,
            LocalDate attendanceDate
    );

    // School-scoped queries: derive school through the class relationship
    List<AttendancePeriod>
    findByClassEntity_School_IdAndAttendanceDateOrderByPeriodNumberAsc(
            Long schoolId,
            LocalDate attendanceDate
    );



    boolean existsByStudent_IdAndClassEntity_IdAndAttendanceDateAndPeriodNumberAndClassEntity_School_Id(
            Long studentId,
            Long classId,
            LocalDate attendanceDate,
            Integer periodNumber,
            Long schoolId
    );


    // Find a period attendance record belonging to the current school
    Optional<AttendancePeriod> findByIdAndClassEntity_School_Id(
            Long id,
            Long schoolId
    );

    // All period records for a student in a specific class
    List<AttendancePeriod>
    findByStudent_IdAndClassEntity_IdAndAttendanceDateOrderByPeriodNumberAsc(
            Long studentId,
            Long classId,
            LocalDate attendanceDate
    );

    // School-scoped class/date-range query
    List<AttendancePeriod>
    findByClassEntity_IdAndClassEntity_School_IdAndAttendanceDateBetweenOrderByAttendanceDateAscPeriodNumberAsc(
            Long classId,
            Long schoolId,
            LocalDate startDate,
            LocalDate endDate
    );

    // Count records for a school, class and date
    long countByClassEntity_IdAndClassEntity_School_IdAndAttendanceDate(
            Long classId,
            Long schoolId,
            LocalDate attendanceDate
    );

    // Check whether a student has any attendance for a period on that date
    boolean existsByStudent_IdAndClassEntity_IdAndAttendanceDateAndPeriodNumber(
            Long studentId,
            Long classId,
            LocalDate attendanceDate,
            Integer periodNumber
    );

    // Parent's children: school-scoped records
    List<AttendancePeriod>
    findByStudent_Parent_IdAndClassEntity_School_IdAndAttendanceDateBetweenOrderByAttendanceDateAscPeriodNumberAsc(
            Long parentId,
            Long schoolId,
            LocalDate startDate,
            LocalDate endDate
    );

}
