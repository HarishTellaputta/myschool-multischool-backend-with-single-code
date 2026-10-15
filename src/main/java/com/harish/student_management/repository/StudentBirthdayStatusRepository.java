package com.harish.student_management.repository;

import com.harish.student_management.entity.StudentBirthdayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentBirthdayStatusRepository
        extends JpaRepository<StudentBirthdayStatus, Long> {

    Optional<StudentBirthdayStatus> findByStudentIdAndBirthdayDate(
            Long studentId,
            LocalDate birthdayDate
    );

    boolean existsByStudentIdAndBirthdayDate(
            Long studentId,
            LocalDate birthdayDate
    );

    List<StudentBirthdayStatus> findByStudentIdOrderByBirthdayDateDesc(
            Long studentId
    );

    @Query("""
        SELECT sbs
        FROM StudentBirthdayStatus sbs
        JOIN FETCH sbs.student s
        WHERE s.parent.id = :parentId
        ORDER BY sbs.birthdayDate DESC
    """)
    List<StudentBirthdayStatus> findAllByParentId(
            @Param("parentId") Long parentId
    );

    @Query("""
        SELECT sbs
        FROM StudentBirthdayStatus sbs
        JOIN FETCH sbs.student s
        WHERE s.parent.id = :parentId
          AND sbs.birthdayDate = :birthdayDate
    """)
    List<StudentBirthdayStatus> findByParentIdAndBirthdayDate(
            @Param("parentId") Long parentId,
            @Param("birthdayDate") LocalDate birthdayDate
    );

    @Query("""
        SELECT sbs
        FROM StudentBirthdayStatus sbs
        JOIN FETCH sbs.student s
        WHERE sbs.id = :id
          AND s.parent.id = :parentId
    """)
    Optional<StudentBirthdayStatus> findByIdAndParentId(
            @Param("id") Long id,
            @Param("parentId") Long parentId
    );
}