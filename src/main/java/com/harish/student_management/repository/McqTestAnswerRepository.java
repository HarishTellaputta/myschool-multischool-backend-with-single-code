package com.harish.student_management.repository;

import com.harish.student_management.entity.McqTestAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface McqTestAnswerRepository extends JpaRepository<McqTestAnswer, Long> {
    Optional<McqTestAnswer> findByAttemptIdAndQuestionId(Long a, Long q);

    List<McqTestAnswer> findByAttemptId(Long attemptId);

    List<McqTestAnswer> findByQuestionId(Long questionId);
}
