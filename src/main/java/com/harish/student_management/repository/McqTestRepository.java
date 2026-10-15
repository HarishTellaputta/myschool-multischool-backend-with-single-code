package com.harish.student_management.repository;

import com.harish.student_management.entity.McqTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.*;
import java.util.*;

public interface McqTestRepository extends JpaRepository<McqTest, Long> {
    List<McqTest> findByTestDate(LocalDate date);

    Optional<McqTest> findByClassEntityIdAndSectionIdAndSubjectAndTestDate(Long c, Long s, String subject, LocalDate d);
    List<McqTest>
    findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime start,
            LocalDateTime end
    );


}
