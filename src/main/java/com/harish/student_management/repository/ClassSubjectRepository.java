package com.harish.student_management.repository;

import com.harish.student_management.entity.ClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassSubjectRepository
        extends JpaRepository<ClassSubject, Long> {

    List<ClassSubject> findByClassEntityId(Long classId);

    List<ClassSubject> findBySubjectId(Long subjectId);

    Optional<ClassSubject> findByClassEntityIdAndSubjectId(
            Long classId,
            Long subjectId
    );

    boolean existsByClassEntityIdAndSubjectId(
            Long classId,
            Long subjectId
    );

    void deleteByClassEntityIdAndSubjectId(
            Long classId,
            Long subjectId
    );
}