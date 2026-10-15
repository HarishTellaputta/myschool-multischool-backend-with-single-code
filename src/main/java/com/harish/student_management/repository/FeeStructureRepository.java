package com.harish.student_management.repository;

import com.harish.student_management.entity.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {
    List<FeeStructure> findByClassEntityId(Long classId);
    boolean existsByNameAndClassEntityIdAndAcademicYearId(
            String name,
            Long classId,
            Long academicYearId
    );
}
