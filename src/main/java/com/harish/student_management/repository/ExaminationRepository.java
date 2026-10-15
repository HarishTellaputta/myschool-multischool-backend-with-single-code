package com.harish.student_management.repository;

import com.harish.student_management.entity.Examination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExaminationRepository extends JpaRepository<Examination, Long> {
    List<Examination> findByAcademicYearId(Long academicYearId);
    Optional<Examination> findByNameIgnoreCase(String name);

    Optional<Examination> findByAcademicYearIdAndNameIgnoreCase(
            Long academicYearId,
            String name
    );
}
