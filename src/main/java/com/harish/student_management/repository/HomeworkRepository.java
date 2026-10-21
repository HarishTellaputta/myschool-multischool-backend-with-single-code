
package com.harish.student_management.repository;

import com.harish.student_management.entity.Homework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HomeworkRepository extends JpaRepository<Homework, Long> {

    // Latest homework first for a class
    List<Homework> findByClassEntityIdOrderByCreatedAtDescIdDesc(
            Long classId
    );

    // Latest homework first for a section
    List<Homework> findBySectionIdOrderByCreatedAtDescIdDesc(
            Long sectionId
    );

    // Latest homework first for a specific class and section
    List<Homework> findByClassEntityIdAndSectionIdOrderByCreatedAtDescIdDesc(
            Long classId,
            Long sectionId
    );

    List<Homework> findByAssignedByTeacherId(
            Long teacherId
    );

    List<Homework> findByAssignedByTeacherIdAndClassEntityId(
            Long teacherId,
            Long classId
    );

    List<Homework> findBySubjectId(
            Long subjectId
    );

    List<Homework> findByClassEntityIdAndSubjectIdOrderByDueDateDesc(
            Long classId,
            Long subjectId
    );

    List<Homework> findByClassEntityIdAndDueDateAfterOrderByCreatedAtDescIdDesc(
            Long classId,
            LocalDate date
    );

    List<Homework> findByClassEntityIdAndSectionIdAndDueDateAfterOrderByCreatedAtDescIdDesc(
            Long classId,
            Long sectionId,
            LocalDate date
    );

    List<Homework> findByStatusOrderByCreatedAtDescIdDesc(
            String status
    );

    List<Homework> findByPriorityOrderByCreatedAtDescIdDesc(
            String priority
    );

    long countByDueDateGreaterThanEqual(
            LocalDate date
    );

    long countByDueDateGreaterThanEqualAndStatus(
            LocalDate date,
            String status
    );

    @Query("""
            SELECT h
            FROM Homework h
            WHERE h.classEntity.id = :classId
              AND h.dueDate BETWEEN :startDate AND :endDate
            ORDER BY h.createdAt DESC, h.id DESC
            """)
    List<Homework> findByClassIdAndDueDateRange(
            @Param("classId") Long classId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT h
            FROM Homework h
            WHERE h.section.id = :sectionId
              AND h.dueDate BETWEEN :startDate AND :endDate
            ORDER BY h.createdAt DESC, h.id DESC
            """)
    List<Homework> findBySectionIdAndDueDateRange(
            @Param("sectionId") Long sectionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT h
            FROM Homework h
            WHERE h.assignedByTeacher.id = :teacherId
              AND h.dueDate BETWEEN :startDate AND :endDate
            ORDER BY h.createdAt DESC, h.id DESC
            """)
    List<Homework> findByTeacherIdAndDueDateRange(
            @Param("teacherId") Long teacherId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    // =========================================================
// SCHOOL-SCOPED HOMEWORK QUERIES
// =========================================================

    @Query("""
    SELECT h FROM Homework h
    WHERE h.id = :id
      AND h.classEntity.school.id = :schoolId
    """)
    Optional<Homework> findByIdAndSchool(
            @Param("id") Long id,
            @Param("schoolId") Long schoolId
    );

    @Query("""
    SELECT h FROM Homework h
    WHERE h.classEntity.school.id = :schoolId
    ORDER BY h.createdAt DESC, h.id DESC
    """)
    List<Homework> findAllBySchoolId(
            @Param("schoolId") Long schoolId
    );

    @Query("""
    SELECT h FROM Homework h
    WHERE h.classEntity.id = :classId
      AND h.classEntity.school.id = :schoolId
    ORDER BY h.createdAt DESC, h.id DESC
    """)
    List<Homework> findByClassAndSchool(
            @Param("classId") Long classId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
    SELECT h FROM Homework h
    WHERE h.section.id = :sectionId
      AND h.classEntity.school.id = :schoolId
    ORDER BY h.createdAt DESC, h.id DESC
    """)
    List<Homework> findBySectionAndSchool(
            @Param("sectionId") Long sectionId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
    SELECT h FROM Homework h
    WHERE h.classEntity.id = :classId
      AND h.section.id = :sectionId
      AND h.classEntity.school.id = :schoolId
    ORDER BY h.createdAt DESC, h.id DESC
    """)
    List<Homework> findByClassSectionAndSchool(
            @Param("classId") Long classId,
            @Param("sectionId") Long sectionId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
    SELECT h FROM Homework h
    WHERE h.assignedByTeacher.id = :teacherId
      AND h.classEntity.school.id = :schoolId
    ORDER BY h.createdAt DESC, h.id DESC
    """)
    List<Homework> findByTeacherAndSchool(
            @Param("teacherId") Long teacherId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
    SELECT h FROM Homework h
    WHERE h.subject.id = :subjectId
      AND h.classEntity.school.id = :schoolId
    ORDER BY h.createdAt DESC, h.id DESC
    """)
    List<Homework> findBySubjectAndSchool(
            @Param("subjectId") Long subjectId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
    SELECT h FROM Homework h
    WHERE h.classEntity.id = :classId
      AND h.subject.id = :subjectId
      AND h.classEntity.school.id = :schoolId
    ORDER BY h.dueDate DESC
    """)
    List<Homework> findByClassSubjectAndSchool(
            @Param("classId") Long classId,
            @Param("subjectId") Long subjectId,
            @Param("schoolId") Long schoolId
    );

    @Query("""
    SELECT h FROM Homework h
    WHERE h.classEntity.id = :classId
      AND h.classEntity.school.id = :schoolId
      AND h.dueDate BETWEEN :startDate AND :endDate
    ORDER BY h.createdAt DESC, h.id DESC
    """)
    List<Homework> findByClassDateRangeAndSchool(
            @Param("classId") Long classId,
            @Param("schoolId") Long schoolId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
    SELECT h FROM Homework h
    WHERE h.classEntity.id = :classId
      AND h.classEntity.school.id = :schoolId
      AND h.dueDate > :date
    ORDER BY h.createdAt DESC, h.id DESC
    """)
    List<Homework> findFutureByClassAndSchool(
            @Param("classId") Long classId,
            @Param("schoolId") Long schoolId,
            @Param("date") LocalDate date
    );


}
