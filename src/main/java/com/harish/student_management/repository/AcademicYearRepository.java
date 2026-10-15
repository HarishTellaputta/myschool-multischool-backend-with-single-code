package com.harish.student_management.repository;

import com.harish.student_management.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    Optional<AcademicYear> findByName(String name);

    Optional<AcademicYear> findByCurrentTrue();

    boolean existsByName(String name);
    Optional<AcademicYear> findByNameIgnoreCase(String name);

}
