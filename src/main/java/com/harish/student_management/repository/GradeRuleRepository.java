package com.harish.student_management.repository;

import com.harish.student_management.entity.GradeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GradeRuleRepository extends JpaRepository<GradeRule, Long> {
    List<GradeRule> findAllByOrderByMinimumPercentageDesc();
}
