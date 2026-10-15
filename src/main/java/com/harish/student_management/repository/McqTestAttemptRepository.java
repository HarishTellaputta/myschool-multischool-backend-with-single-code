package com.harish.student_management.repository;

import com.harish.student_management.entity.McqTestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface McqTestAttemptRepository extends JpaRepository<McqTestAttempt, Long> {
    Optional<McqTestAttempt> findByTestIdAndStudentId(Long testId, Long studentId);

    List<McqTestAttempt> findByStudentIdOrderByStartedAtDesc(Long studentId);

    List<McqTestAttempt> findByTestClassEntityId(Long classId);

    Page<McqTestAttempt> findByStartedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("""
    SELECT a
    FROM McqTestAttempt a
    JOIN a.test t
    WHERE (:date IS NULL OR t.testDate = :date)
      AND (:classId IS NULL OR t.classEntity.id = :classId)
      AND (:sectionId IS NULL OR t.section.id = :sectionId)
      AND (:subject IS NULL OR LOWER(t.subject) = LOWER(:subject))
      AND (:from IS NULL OR a.startedAt >= :from)
      AND (:to IS NULL OR a.startedAt <= :to)
""")
    Page<McqTestAttempt> findPerformanceReport(
            @Param("date") LocalDate date,
            @Param("classId") Long classId,
            @Param("sectionId") Long sectionId,
            @Param("subject") String subject,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
    List<McqTestAttempt> findByTestId(Long testId);
}
