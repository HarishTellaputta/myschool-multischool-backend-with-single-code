
        package com.harish.student_management.repository;

import com.harish.student_management.entity.TeacherLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherLeaveRepository
        extends JpaRepository<TeacherLeave, Long> {

    // All leaves belonging to the current school
    List<TeacherLeave> findAllByTeacher_School_IdOrderByStartDateDesc(
            Long schoolId
    );

    // Find one leave only within the current school
    Optional<TeacherLeave> findByIdAndTeacher_School_Id(
            Long id,
            Long schoolId
    );

    // All leaves for one teacher within the current school
    List<TeacherLeave> findAllByTeacher_IdAndTeacher_School_IdOrderByStartDateDesc(
            Long teacherId,
            Long schoolId
    );

    // Check whether the teacher already has an overlapping leave
    boolean existsByTeacher_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long teacherId,
            LocalDate endDate,
            LocalDate startDate
    );

    // Check overlap while updating, excluding the current leave
    boolean existsByTeacher_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
            Long teacherId,
            LocalDate endDate,
            LocalDate startDate,
            Long excludeLeaveId
    );

    // All leaves in the school, without sorting
    List<TeacherLeave> findAllByTeacher_School_Id(
            Long schoolId
    );

    // All leaves for one teacher in the school, without sorting
    List<TeacherLeave> findAllByTeacher_IdAndTeacher_School_Id(
            Long teacherId,
            Long schoolId
    );

    // Leaves starting within a date range
    List<TeacherLeave> findAllByTeacher_School_IdAndStartDateBetween(
            Long schoolId,
            LocalDate startDate,
            LocalDate endDate
    );

    // School leaves overlapping a selected period
    List<TeacherLeave>
    findAllByTeacher_School_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long schoolId,
            LocalDate periodEnd,
            LocalDate periodStart
    );

    // School-admin: leaves overlapping a selected period
    List<TeacherLeave>
    findAllByTeacher_School_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
            Long schoolId,
            LocalDate periodEnd,
            LocalDate periodStart
    );

    // Teacher: own leaves overlapping a selected period
    List<TeacherLeave>
    findAllByTeacher_IdAndTeacher_School_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
            Long teacherId,
            Long schoolId,
            LocalDate periodEnd,
            LocalDate periodStart
    );
}
