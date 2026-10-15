package com.harish.student_management.repository;

import com.harish.student_management.entity.McqQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface McqQuestionRepository
        extends JpaRepository<McqQuestion, Long> {

    List<McqQuestion> findByQuestionDate(
            LocalDate questionDate
    );

    List<McqQuestion> findByClassEntityId(
            Long classId
    );

    List<McqQuestion> findByQuestionDateAndClassEntityId(
            LocalDate date,
            Long classId
    );

    List<McqQuestion> findBySubjectId(
            Long subjectId
    );

    List<McqQuestion> findByClassEntityIdAndSubjectId(
            Long classId,
            Long subjectId
    );

    List<McqQuestion>
    findByQuestionDateAndClassEntityIdAndSubjectId(
            LocalDate questionDate,
            Long classId,
            Long subjectId
    );

    List<McqQuestion> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime start,
            LocalDateTime end
    );
}