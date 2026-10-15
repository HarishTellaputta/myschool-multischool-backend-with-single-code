package com.harish.student_management.repository;

import com.harish.student_management.entity.TeacherLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TeacherLeaveRepository extends JpaRepository<TeacherLeave, Long> {

    List<TeacherLeave> findByTeacherId(Long teacherId);

    List<TeacherLeave> findByTeacherIdAndStatus(Long teacherId, String status);

    List<TeacherLeave> findByStatus(String status);

    List<TeacherLeave> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByTeacherIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long teacherId, LocalDate endDate, LocalDate startDate);
}
