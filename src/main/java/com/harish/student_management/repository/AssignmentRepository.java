package com.harish.student_management.repository;

import com.harish.student_management.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByClassEntityId(Long classId);

    List<Assignment> findBySectionId(Long sectionId);

    List<Assignment> findByClassEntityIdAndSectionId(
            Long classId,
            Long sectionId
    );

    List<Assignment> findByAssignedByTeacherId(Long teacherId);

    List<Assignment> findByAssignedByTeacherIdAndClassEntityId(
            Long teacherId,
            Long classId
    );

    // Subject master
    List<Assignment> findBySubjectId(Long subjectId);

    List<Assignment> findByClassEntityIdAndSubjectIdOrderByDueDate(
            Long classId,
            Long subjectId
    );

    List<Assignment> findByClassEntityIdAndDueDateAfter(
            Long classId,
            LocalDate date
    );

    List<Assignment> findByClassEntityIdAndSectionIdAndDueDateAfter(
            Long classId,
            Long sectionId,
            LocalDate date
    );

    List<Assignment> findByStatus(String status);

    List<Assignment> findByPriority(String priority);

    List<Assignment> findByAssignedDateBetween(
            LocalDate startDate,
            LocalDate endDate
    );

    List<Assignment> findByDueDateBetween(
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("""
            SELECT a
            FROM Assignment a
            WHERE a.classEntity.id = :classId
              AND a.dueDate BETWEEN :startDate AND :endDate
            """)
    List<Assignment> findByClassIdAndDueDateRange(
            @Param("classId") Long classId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT a
            FROM Assignment a
            WHERE a.section.id = :sectionId
              AND a.dueDate BETWEEN :startDate AND :endDate
            """)
    List<Assignment> findBySectionIdAndDueDateRange(
            @Param("sectionId") Long sectionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT a
            FROM Assignment a
            WHERE a.assignedByTeacher.id = :teacherId
              AND a.dueDate BETWEEN :startDate AND :endDate
            """)
    List<Assignment> findByTeacherIdAndDueDateRange(
            @Param("teacherId") Long teacherId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT a
            FROM Assignment a
            WHERE a.status = :status
              AND a.dueDate < :date
            """)
    List<Assignment> findByStatusAndDueDateBefore(
            @Param("status") String status,
            @Param("date") LocalDate date
    );
}