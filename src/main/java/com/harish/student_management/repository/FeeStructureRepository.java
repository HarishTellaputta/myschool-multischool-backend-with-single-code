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

    List<FeeStructure> findAllByClassEntity_School_Id(Long schoolId);

    List<FeeStructure> findAllByClassEntity_School_IdAndClassEntity_Id(
            Long schoolId, Long classId);

    Optional<FeeStructure> findByIdAndClassEntity_School_Id(
            Long id, Long schoolId);

    boolean existsByNameIgnoreCaseAndClassEntity_IdAndAcademicYear_Id(
            String name, Long classId, Long academicYearId);



    boolean existsByNameIgnoreCaseAndClassEntity_IdAndAcademicYearIsNull(
            String name,
            Long classId
    );
}
