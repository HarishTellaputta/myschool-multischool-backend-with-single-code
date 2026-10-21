
package com.harish.student_management.repository;

import com.harish.student_management.entity.GradeRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradeRuleRepository
        extends JpaRepository<GradeRule, Long> {

    List<GradeRule> findAllBySchool_IdOrderByMinimumPercentageDesc(
            Long schoolId
    );


    List<GradeRule> findAllBySchool_Id(Long schoolId);

    Optional<GradeRule> findByIdAndSchool_Id(
            Long id,
            Long schoolId
    );

    boolean existsBySchool_IdAndGradeIgnoreCase(
            Long schoolId,
            String grade
    );

    Optional<GradeRule> findBySchool_IdAndGradeIgnoreCase(
            Long schoolId,
            String grade
    );

}
